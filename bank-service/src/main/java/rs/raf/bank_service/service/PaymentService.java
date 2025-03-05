package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.CreatePaymentDto;
import rs.raf.bank_service.domain.dto.PaymentVerificationRequestDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.CurrencyType;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PaymentService {

    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;

    public Long getClientIdFromToken(String token) {
        token = token.replace("Bearer ", "");
        if (!jwtTokenUtil.validateToken(token)) {
            throw new SecurityException("Invalid token");
        }
        return Long.valueOf(jwtTokenUtil.extractUserId(token));
    }

    public boolean createTransferPendingConformation(TransferDto transferDto, Long clientId) {
        // Preuzimanje računa za sender i receiver
        Account sender = accountRepository.findByAccountNumber(transferDto.getSenderAccountNumber()).stream().findFirst().orElseThrow(() -> new SenderAccountNotFoundException(transferDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(transferDto.getReceiverAccountNumber()).stream().findFirst().orElseThrow(() -> new ReceiverAccountNotFoundException(transferDto.getReceiverAccountNumber()));

        // Provera da li sender ima dovoljno sredstava
        if (sender.getBalance().compareTo(transferDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), transferDto.getAmount());
        }

        // Provera da li su računi isti tip valute
        if (!(sender.getCurrency().equals(receiver.getCurrency()))) {
            throw new NotSameCurrencyForTransferException(sender.getCurrency().toString(), receiver.getCurrency().toString());
        }

        // Kreiranje Payment entiteta za transfer
        Payment payment = new Payment();
        payment.setClintId(clientId);  // Dodajemo Client ID
        payment.setSenderAccount(sender);  // Sender račun
        payment.setAmount(transferDto.getAmount());  // Iznos
        payment.setAccountNumberReciver(transferDto.getReceiverAccountNumber());  // Primalac (receiver)
        payment.setPaymentStatus(PaymentStatus.PENDING_CONFORMATION);  // Status je "na čekanju"
        payment.setTransactionDate(LocalDateTime.now());  // Datum transakcije

        paymentRepository.save(payment);

        // Kreiraj PaymentVerificationRequestDto i pozovi UserClient da kreira verificationRequest
        PaymentVerificationRequestDto paymentVerificationRequestDto = new PaymentVerificationRequestDto(clientId, payment.getId(), "Transfer");
        userClient.createTransferRequest(paymentVerificationRequestDto);

        return true;
    }

    public boolean confirmTransferAndExecute(Long paymentId, Long clientId) {
        // Preuzimanje payment entiteta na osnovu paymentId
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Provera da li se ClientId poklapa sa ClientId iz payment-a
        if (!payment.getClintId().equals(clientId)) {
            throw new UnauthorizedTransferConormationException(clientId, payment.getClintId());
        }

        // Preuzimanje računa za sender i receiver koristeći podatke iz payment-a
        Account sender = payment.getSenderAccount();
        Account receiver = accountRepository.findByAccountNumber(payment.getAccountNumberReciver()).stream().findFirst().orElseThrow(() -> new ReceiverAccountNotFoundException(payment.getAccountNumberReciver()));

        // Oduzimanje novca sa sender računa i dodavanje na receiver račun
        sender.setBalance(sender.getBalance().subtract(payment.getAmount()));
        receiver.setBalance(receiver.getBalance().add(payment.getAmount()));

        // Snimanje izmena u bazi
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Ažuriranje statusa payment-a na "COMPLETED"
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        return true;
    }


    public boolean createPaymentBeforeConformation(CreatePaymentDto paymentDto, Long clientId) {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }

        // Preuzimanje sender računa
        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber()).stream().findFirst().orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        // Provera valute
        if (!(sender.getCurrency().getCode().equals(CurrencyType.RSD.toString()))) {
            throw new SendersAccountsCurencyIsNotDinarException();
        }

        // Provera balansa sender računa
        if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
        }

        // Kreiranje Payment entiteta
        Payment payment = new Payment();
        payment.setSenderName(paymentDto.getSenderName());
        payment.setClintId(clientId);
        payment.setSenderAccount(sender);
        payment.setAccountNumberReciver(paymentDto.getReceiverAccountNumber().toString());
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setTransactionDate(LocalDateTime.now());
        payment.setPaymentStatus(PaymentStatus.PENDING_CONFORMATION);
        paymentRepository.save(payment);

        PaymentVerificationRequestDto paymentVerificationRequestDto = new PaymentVerificationRequestDto(clientId, payment.getId(), "Payment");
        userClient.createPaymentRequest(paymentVerificationRequestDto);

        return true;
    }


    public void confirmPayment(Long paymentId, Long clientId) {
        // Preuzimanje payment entiteta na osnovu paymentId
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Provera da li se clientId poklapa sa clientId iz payment-a
        if (!payment.getClintId().equals(clientId)) {
            throw new UnauthorizedPaymentException(clientId, payment.getClintId());
        }

        // Preuzimanje sender i receiver računa
        Account sender = payment.getSenderAccount();
        String receiverString = payment.getAccountNumberReciver();

        Account receiver = (Account) accountRepository.findAllByAccountNumber(receiverString);

        // Ako je receiver u banci, izvrši transakciju
        if (receiver != null) {
            // Konverzija iznos sa RSD u valutu primaoca
            BigDecimal convertedAmount = convert(payment.getAmount(), CurrencyType.valueOf(receiver.getCurrency().getCode()));

            // Dodavanje iznosa na receiver račun u odgovarajućoj valuti
            receiver.setBalance(receiver.getBalance().add(convertedAmount));
            accountRepository.save(receiver);
        }

        sender.setBalance(sender.getBalance().subtract(payment.getAmount()));
        accountRepository.save(sender);

        // Ažuriranje statusa payment-a na "COMPLETED"
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
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
}

