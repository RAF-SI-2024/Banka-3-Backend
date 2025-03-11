package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.CurrencyType;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.enums.VerificationType;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.mapper.PaymentMapper;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.specification.PaymentSpecification;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentService {

    private final AccountRepository accountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private PaymentRepository paymentRepository;
    private CardRepository cardRepository;
    private final UserClient userClient;
    private final PaymentMapper paymentMapper;
    private final ExchangeRateService exchangeRateService;

    /**
     * Kreira transfer između dva računa. Ako su valute različite, koristi se konverzija preko RSD.
     */
    public boolean createTransferPendingConfirmation(TransferDto transferDto, Long clientId) {
        Account sender = accountRepository.findByAccountNumber(transferDto.getSenderAccountNumber())
                .orElseThrow(() -> new SenderAccountNotFoundException(transferDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(transferDto.getReceiverAccountNumber())
                .orElseThrow(() -> new ReceiverAccountNotFoundException(transferDto.getReceiverAccountNumber()));

        BigDecimal amount = transferDto.getAmount();
        BigDecimal convertedAmount = amount;

        // Koristimo ExchangeRateService da dobijemo konvertovani iznos
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            convertedAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode())
                    .multiply(amount);
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), amount);
        }

        // Kreiranje payment entiteta
        Payment payment = new Payment();
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAmount(amount);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setDate(LocalDateTime.now());
        payment.setReceiverClientId(receiver.getClientId());

        paymentRepository.save(payment);

        // Kreiranje verifikacije
        CreateVerificationRequestDto verificationRequest = new CreateVerificationRequestDto(clientId, payment.getId(), VerificationType.TRANSFER);
        userClient.createVerificationRequest(verificationRequest);

        return true;
    }

    /**
     * Potvrđuje transfer i obavlja stvarnu transakciju sredstava između računa.
     */
    public boolean confirmTransferAndExecute(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();
        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver())
                .orElseThrow(() -> new ReceiverAccountNotFoundException(payment.getAccountNumberReceiver()));

        BigDecimal amount = payment.getAmount();
        BigDecimal convertedAmount = amount;

        // KORISTIMO ExchangeRateService
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            convertedAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode())
                    .multiply(amount);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(convertedAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        return true;
    }


    /**
     * Kreira uplatu koja čeka potvrdu.
     */
    public boolean createPaymentBeforeConfirmation(CreatePaymentDto paymentDto, Long clientId) {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }

        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber())
                .orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber())
                .orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));

        BigDecimal amount = paymentDto.getAmount();
        BigDecimal convertedAmount = amount;

        // **KORISTIMO ExchangeRateService**
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            convertedAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode())
                    .multiply(amount);
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), amount);
        }

        Payment payment = new Payment();
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());
        payment.setAmount(amount);
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setReceiverClientId(receiver.getClientId());

        paymentRepository.save(payment);

        CreateVerificationRequestDto verificationRequest = new CreateVerificationRequestDto(clientId, payment.getId(), VerificationType.PAYMENT);
        userClient.createVerificationRequest(verificationRequest);

        return true;
    }

    /**
     * Potvrđuje uplatu i izvršava transakciju.
     */
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        Account sender = payment.getSenderAccount();
        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver())
                .orElseThrow(() -> new ReceiverAccountNotFoundException(payment.getAccountNumberReceiver()));

        BigDecimal amount = payment.getAmount();
        BigDecimal convertedAmount = amount;


        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            convertedAmount = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode())
                    .multiply(amount);
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(convertedAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);
    }


    public static BigDecimal convert(@NotNull(message = "Amount is required.") @Positive(message = "Amount must be positive.") BigDecimal amountInRSD, CurrencyType currencyType) {
        BigDecimal convertedAmount = BigDecimal.ZERO;  // Postavi početnu vrednost kao 0

        if (currencyType == CurrencyType.RSD) {
            convertedAmount = amountInRSD;
        } else if (currencyType == CurrencyType.EUR) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0085"));
        } else if (currencyType == CurrencyType.USD) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.010"));
        } else if (currencyType == CurrencyType.HRK) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.064"));
        } else if (currencyType == CurrencyType.JPY) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("1.14"));
        } else if (currencyType == CurrencyType.GBP) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0076"));
        } else if (currencyType == CurrencyType.AUD) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.014"));
        } else if (currencyType == CurrencyType.CHF) {
            convertedAmount = amountInRSD.multiply(new BigDecimal("0.0095"));
        } else {
            throw new CurrencyNotFoundException(currencyType.toString());
        }
        return convertedAmount;
    }

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

    // Dohvatanje detalja transakcije po ID-u
    public PaymentDetailsDto getPaymentDetails(String token, Long id) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        Payment payment = paymentRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return paymentMapper.toDetailsDto(payment);
    }


}
