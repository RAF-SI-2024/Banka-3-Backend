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
        BigDecimal exchangeRateValue = BigDecimal.ONE;

        // Ako su valute različite, koristimo kurs i idemo preko bankovnih računa
        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            Account bankAccountFrom = accountRepository.findFirstByCurrency(sender.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + sender.getCurrency().getCode()));

            Account bankAccountTo = accountRepository.findFirstByCurrency(receiver.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + receiver.getCurrency().getCode()));

            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getExchangeRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            // Sender  Banka
            sender.setBalance(sender.getBalance().subtract(amount));
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().add(amount));
            accountRepository.save(sender);
            accountRepository.save(bankAccountFrom);

            // Banka  Banka (konverzija)
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().subtract(convertedAmount));
            bankAccountTo.setBalance(bankAccountTo.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountFrom);
            accountRepository.save(bankAccountTo);

            // Banka  Receiver
            bankAccountTo.setBalance(bankAccountTo.getBalance().subtract(convertedAmount));
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountTo);
        } else {
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));
        }

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Kreiranje payment entiteta i čuvanje kursa
        Payment payment = new Payment();
        payment.setClientId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReceiver(receiver.getAccountNumber());
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setDate(LocalDateTime.now());
        payment.setReceiverClientId(receiver.getClientId());
        payment.setExchangeRate(exchangeRateValue); // Čuvamo kurs

        paymentRepository.save(payment);

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
        BigDecimal exchangeRateValue = BigDecimal.ONE;


        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {

            Account bankAccountFrom = accountRepository.findFirstByCurrency(sender.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + sender.getCurrency().getCode()));

            Account bankAccountTo = accountRepository.findFirstByCurrency(receiver.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + receiver.getCurrency().getCode()));


            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getExchangeRate();
            convertedAmount = amount.multiply(exchangeRateValue);


            sender.setBalance(sender.getBalance().subtract(amount));
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().add(amount));
            accountRepository.save(sender);
            accountRepository.save(bankAccountFrom);


            bankAccountFrom.setBalance(bankAccountFrom.getBalance().subtract(convertedAmount));
            bankAccountTo.setBalance(bankAccountTo.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountFrom);
            accountRepository.save(bankAccountTo);


            bankAccountTo.setBalance(bankAccountTo.getBalance().subtract(convertedAmount));
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountTo);
        } else {

            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));
        }

        accountRepository.save(sender);
        accountRepository.save(receiver);


        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setExchangeRate(exchangeRateValue); // Čuvamo kurs u trenutku transakcije
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
        BigDecimal exchangeRateValue = BigDecimal.ONE;

        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            Account bankAccountFrom = accountRepository.findFirstByCurrency(sender.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + sender.getCurrency().getCode()));

            Account bankAccountTo = accountRepository.findFirstByCurrency(receiver.getCurrency())
                    .orElseThrow(() -> new BankAccountNotFoundException("No bank account found for currency: " + receiver.getCurrency().getCode()));

            ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(sender.getCurrency().getCode(), receiver.getCurrency().getCode());
            exchangeRateValue = exchangeRateDto.getExchangeRate();
            convertedAmount = amount.multiply(exchangeRateValue);

            sender.setBalance(sender.getBalance().subtract(amount));
            bankAccountFrom.setBalance(bankAccountFrom.getBalance().add(amount));
            accountRepository.save(sender);
            accountRepository.save(bankAccountFrom);

            bankAccountFrom.setBalance(bankAccountFrom.getBalance().subtract(convertedAmount));
            bankAccountTo.setBalance(bankAccountTo.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountFrom);
            accountRepository.save(bankAccountTo);

            bankAccountTo.setBalance(bankAccountTo.getBalance().subtract(convertedAmount));
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(bankAccountTo);
        } else {
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));
        }

        accountRepository.save(sender);
        accountRepository.save(receiver);


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
        payment.setExchangeRate(exchangeRateValue); // Čuvamo kurs

        paymentRepository.save(payment);

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
        BigDecimal exchangeRateValue = payment.getExchangeRate(); // Koristimo kurs iz baze

        if (!sender.getCurrency().getCode().equals(receiver.getCurrency().getCode())) {
            convertedAmount = amount.multiply(exchangeRateValue);
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
