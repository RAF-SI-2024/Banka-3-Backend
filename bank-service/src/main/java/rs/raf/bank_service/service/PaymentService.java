package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.PaymentDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.CurrencyType;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.PaymentRepository;

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

    public boolean transferFunds(TransferDto paymentDto) {
        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber()).stream().findFirst().orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber()).stream().findFirst().orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));

        if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
        }

        if (!(sender.getCurrency().equals(receiver.getCurrency()))) {
            throw new NotSameCurrencyForTransferException(sender.getCurrency().toString(), receiver.getCurrency().toString());
        }

        sender.setBalance(sender.getBalance().subtract(paymentDto.getAmount()));
        receiver.setBalance(receiver.getBalance().add(paymentDto.getAmount()));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        return true;
    }

    public boolean makePayment(PaymentDto paymentDto) {
        if (paymentDto.getPaymentCode() == null || paymentDto.getPaymentCode().isEmpty()) {
            throw new PaymentCodeNotProvidedException();
        }

        if (paymentDto.getPurposeOfPayment() == null || paymentDto.getPurposeOfPayment().isEmpty()) {
            throw new PurposeOfPaymentNotProvidedException();
        }

        Account sender = accountRepository.findByAccountNumber(paymentDto.getSenderAccountNumber())
                .stream()
                .findFirst()
                .orElseThrow(() -> new SenderAccountNotFoundException(paymentDto.getSenderAccountNumber()));

        if (!(sender.getCurrency().getCode().equals(CurrencyType.RSD.toString()))) {
            throw new SendersAccountsCurencyIsNotDinarException();
        }

        Account receiver = accountRepository.findByAccountNumber(paymentDto.getReceiverAccountNumber())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReceiverAccountNotFoundException(paymentDto.getReceiverAccountNumber()));

        if (sender.getBalance().compareTo(paymentDto.getAmount()) < 0) {
            throw new InsufficientFundsException(sender.getBalance(), paymentDto.getAmount());
        }

        BigDecimal amountInOtherCurrency = convert(paymentDto.getAmount(), CurrencyType.valueOf(receiver.getCurrency().getCode()));

        Payment payment = new Payment();
        payment.setSenderAccount(sender);
        payment.setReceiverAccount(receiver);
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentCode(paymentDto.getPaymentCode());
        payment.setPurposeOfPayment(paymentDto.getPurposeOfPayment());
        payment.setReferenceNumber(paymentDto.getReferenceNumber());
        payment.setTransactionDate(LocalDateTime.now());

        paymentRepository.save(payment);

        sender.setBalance(sender.getBalance().subtract(paymentDto.getAmount()));
        accountRepository.save(sender);

        receiver.setBalance(receiver.getBalance().add(amountInOtherCurrency));
        accountRepository.save(receiver);

        return true;
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

