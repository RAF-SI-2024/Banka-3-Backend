package rs.raf.bank_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.enums.TransactionType;
import rs.raf.bank_service.domain.enums.VerificationType;
import rs.raf.bank_service.domain.mapper.PaymentMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.repository.CompanyAccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.specification.PaymentSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final ExchangeRateService exchangeRateService;
    private final TransactionQueueService transactionQueueService;
    private PaymentRepository paymentRepository;
    private CardRepository cardRepository;
    private CompanyAccountRepository companyAccountRepository;

    // Dohvatanje svih transakcija za određenog klijenta sa filtriranjem
    public Page<PaymentOverviewDto> getPayments(
            String token,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            PaymentStatus paymentStatus,
            String accountNumber,
            String cardNumber,
            Pageable pageable
    ) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);

        if (accountNumber != null) {
            accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(AccountNotFoundException::new);
        }

        if (cardNumber != null) {
            cardRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new CardNotFoundException(cardNumber));
        }

        Specification<Payment> spec = PaymentSpecification.filterPayments(clientId, startDate, endDate, minAmount, maxAmount, paymentStatus, accountNumber, cardNumber);
        Page<Payment> payments = paymentRepository.findAll(spec, pageable);
        return payments.map(paymentMapper::toOverviewDto);
    }

    public PaymentDetailsDto getPaymentDetails(String token, Long id) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        Payment payment = paymentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return paymentMapper.toDetailsDto(payment);
    }

    public void rejectPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getStatus().equals(PaymentStatus.PENDING_CONFIRMATION))
            throw new RejectNonPendingRequestException();

        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);

        Account sender = payment.getSenderAccount();
        sender.setAvailableBalance(sender.getAvailableBalance().add(payment.getAmount()));

        accountRepository.save(sender);
    }

    public BigDecimal getExchangeProfit() {
        return paymentRepository.getBankProfitFromExchange();
    }

    @Transactional
    public PaymentDto createTransferAndVerificationRequest(TransferDto transferDto, Long clientId) throws JsonProcessingException {
        if (!getReceiverAccount(transferDto.getReceiverAccountNumber()).getClientId().equals(clientId)) {
            throw new AccountNotFoundException();
        }

        Payment payment = createPayment(CreatePaymentDto.builder()
                        .senderAccountNumber(transferDto.getSenderAccountNumber())
                        .receiverAccountNumber(transferDto.getReceiverAccountNumber())
                        .amount(transferDto.getAmount())
                        .build(),
                clientId);

        createPaymentVerificationRequest(payment, clientId);

        return paymentMapper.toPaymentDto(payment, "Receiver");
    }

    @Transactional
    public PaymentDetailsDto createAndExecuteSystemPayment(CreatePaymentDto paymentDto, Long clientId) throws JsonProcessingException {
        Payment payment = createPayment(paymentDto, clientId);

        return confirmPayment(payment.getId());
    }

    public PaymentDto createPaymentAndVerificationRequest(CreatePaymentDto createPaymentDto, Long clientId) throws JsonProcessingException {
        validatePaymentData(createPaymentDto);
        Payment payment = createPayment(createPaymentDto, clientId);
        createPaymentVerificationRequest(payment, clientId);

//        Account receiver = getReceiverAccount(createPaymentDto.getReceiverAccountNumber());

        return paymentMapper.toPaymentDto(payment, "Receiver");
    }

    private Payment createPayment(CreatePaymentDto paymentDto, Long clientId) throws JsonProcessingException {
        Account sender = getSenderAccount(paymentDto.getSenderAccountNumber(), clientId);
        Account receiver = getReceiverAccount(paymentDto.getReceiverAccountNumber());

        validateSufficientFunds(sender, paymentDto.getAmount());

        updateAccountBalance(sender, sender.getBalance(), sender.getAvailableBalance().subtract(paymentDto.getAmount()));

        String senderNameSurname = getSenderName(sender);

        // Handling currency conversion
        CurrencyConversionResult conversionResult = calculateCurrencyConversion(
                sender.getCurrency(),
                receiver.getCurrency(),
                paymentDto.getAmount());

        // Create payment entity
        Payment payment = createPaymentEntity(
                sender,
                paymentDto,
                senderNameSurname,
                conversionResult.getConvertedAmount(),
                conversionResult.getExchangeProfit(),
                clientId
        );

        payment.setReceiverClientId(receiver.getClientId());
        paymentRepository.save(payment);

        return payment;
    }

    private void createPaymentVerificationRequest(Payment payment, Long clientId) throws JsonProcessingException {
        PaymentVerificationDetailsDto paymentVerificationDetailsDto = PaymentVerificationDetailsDto.builder()
                .fromAccountNumber(payment.getSenderAccount().getAccountNumber())
                .toAccountNumber(payment.getAccountNumberReceiver())
                .amount(payment.getAmount())
                .build();

        CreateVerificationRequestDto createVerificationRequestDto = new CreateVerificationRequestDto(
                clientId,
                payment.getId(),
                VerificationType.PAYMENT,
                objectMapper.writeValueAsString(paymentVerificationDetailsDto)
        );

        userClient.createVerificationRequest(createVerificationRequestDto);
    }

    @Transactional
    public PaymentDetailsDto confirmPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);

        if (!payment.getStatus().equals(PaymentStatus.PENDING_CONFIRMATION))
            throw new RejectNonPendingRequestException();

        Account sender = payment.getSenderAccount();
        Account receiver = getReceiverAccount(payment.getAccountNumberReceiver());

        processPaymentWithCurrencyHandling(payment, sender, receiver);

        payment.setStatus(PaymentStatus.COMPLETED);
        return paymentMapper.toDetailsDto(paymentRepository.save(payment));
    }

    private Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private Account getSenderAccount(String accountNumber, Long clientId) {
        return accountRepository.findByAccountNumberAndClientId(accountNumber, clientId)
                .stream().findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(accountNumber));
    }

    private Account getReceiverAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .stream().findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(accountNumber));
    }

    private void validateSufficientFunds(Account sender, BigDecimal amount) {
        if (sender.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sender.getAvailableBalance(), amount);
        }
    }

    private void validatePaymentData(CreatePaymentDto paymentDto) {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }
    }

    private String getSenderName(Account sender) {
        if (sender instanceof CompanyAccount && ((CompanyAccount) sender).getCompanyId() == 1) {
            return "Banka 2";
        } else {
            ClientDto clientDto = userClient.getClientById(sender.getClientId());
            return clientDto.getFirstName() + " " + clientDto.getLastName();
        }
    }

    private Payment createPaymentEntity(Account sender, CreatePaymentDto paymentDto, String senderName,
                                        BigDecimal convertedAmount, BigDecimal exchangeProfit, Long clientId) {
        Payment payment = new Payment();
        payment.setSenderName(senderName);
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(paymentDto.getReceiverAccountNumber());
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setOutAmount(convertedAmount);
        payment.setExchangeProfit(exchangeProfit);

        return payment;
    }

    @Getter
    @AllArgsConstructor
    private static class CurrencyConversionResult {
        private final BigDecimal convertedAmount;
        private final BigDecimal exchangeRateValue;
        private final BigDecimal exchangeProfit;
    }

    private CurrencyConversionResult calculateCurrencyConversion(Currency senderCurrency, Currency receiverCurrency, BigDecimal amount) {
        BigDecimal convertedAmount = amount;
        BigDecimal exchangeRateValue = BigDecimal.ONE;
        BigDecimal exchangeProfit = BigDecimal.ZERO;

        if (!senderCurrency.equals(receiverCurrency)) {
            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(
                    senderCurrency.getCode(),
                    receiverCurrency.getCode());

            exchangeRateValue = exchangeRateDto.getSellRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            exchangeProfit = exchangeRateDto.getExchangeRate().multiply(amount)
                    .subtract(exchangeRateDto.getSellRate().multiply(amount));
        }

        return new CurrencyConversionResult(convertedAmount, exchangeRateValue, exchangeProfit);
    }

    private void processPaymentWithCurrencyHandling(Payment payment, Account sender, Account receiver) {
        BigDecimal amount = payment.getAmount();
        BigDecimal convertedAmount = payment.getOutAmount();

        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            processDifferentCurrencyPayment(sender, receiver, amount, convertedAmount);
        } else {
            processSameCurrencyPayment(sender, receiver, amount);
        }
    }

    private void processDifferentCurrencyPayment(Account sender, Account receiver, BigDecimal amount, BigDecimal convertedAmount) {
        CompanyAccount bankAccountFrom = accountRepository.findFirstByCurrencyAndCompanyId(sender.getCurrency(), 1L)
                .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + sender.getCurrency().getCode()));

        CompanyAccount bankAccountTo = accountRepository.findFirstByCurrencyAndCompanyId(receiver.getCurrency(), 1L)
                .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + receiver.getCurrency().getCode()));

        // Sender -> Bank (same currency)
        updateAccountBalance(sender, sender.getBalance().subtract(amount));
        updateAccountBalance(bankAccountFrom, bankAccountFrom.getBalance().add(amount), bankAccountFrom.getAvailableBalance().add(amount));

        // Bank -> Receiver (converted currency)
        updateAccountBalance(bankAccountTo, bankAccountTo.getBalance().subtract(convertedAmount), bankAccountTo.getAvailableBalance().subtract(convertedAmount));
        updateAccountBalance(receiver, receiver.getBalance().add(convertedAmount), receiver.getAvailableBalance().add(convertedAmount));
    }

    private void processSameCurrencyPayment(Account sender, Account receiver, BigDecimal amount) {
        updateAccountBalance(sender, sender.getBalance().subtract(amount));
        updateAccountBalance(receiver, receiver.getBalance().add(amount), receiver.getAvailableBalance().add(amount));
    }

    private void updateAccountBalance(Account account, BigDecimal newBalance) {
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    private void updateAccountBalance(Account account, BigDecimal newBalance, BigDecimal newAvailableBalance) {
        account.setBalance(newBalance);
        account.setAvailableBalance(newAvailableBalance);
        accountRepository.save(account);
    }
}
