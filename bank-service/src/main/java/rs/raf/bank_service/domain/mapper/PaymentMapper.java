package rs.raf.bank_service.domain.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.repository.AccountRepository;

import java.util.Objects;

@Component
@AllArgsConstructor
public class PaymentMapper {
    private final AccountRepository accountRepository;

    public PaymentOverviewDto toOverviewDto(Payment payment) {
        PaymentOverviewDto dto = new PaymentOverviewDto();
        dto.setId(payment.getId());
        dto.setSenderName(payment.getSenderName());
        dto.setDate(payment.getDate());
        dto.setStatus(payment.getStatus());

        dto.setAmount(payment.getAmount());
        dto.setReceivedAmount(payment.getOutAmount());
        dto.setSenderCurrencyCode(payment.getSenderAccount().getCurrency().getCode());

        Account receiverAccount = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver()).orElse(null);
        dto.setReceiverCurrencyCode(receiverAccount != null ? receiverAccount.getCurrency().getCode() : null);

        // Dodajte podatke o kartici
        if (payment.getCard() != null) {
            dto.setCardNumber(payment.getCard().getCardNumber());
        } else {
            dto.setCardNumber(null);
        }

        return dto;
    }

    public PaymentDetailsDto toDetailsDto(Payment payment) {
        PaymentDetailsDto dto = new PaymentDetailsDto();
        dto.setId(payment.getId());
        dto.setSenderName(payment.getSenderName());
        dto.setAccountNumberReceiver(payment.getAccountNumberReceiver());
        dto.setPaymentCode(payment.getPaymentCode());
        dto.setPurposeOfPayment(payment.getPurposeOfPayment());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setDate(payment.getDate());
        dto.setStatus(payment.getStatus());

        dto.setAmount(payment.getAmount());
        dto.setReceivedAmount(payment.getOutAmount());
        dto.setSenderCurrencyCode(payment.getSenderAccount().getCurrency().getCode());

        Account receiverAccount = accountRepository.findByAccountNumber(payment.getAccountNumberReceiver()).orElse(null);
        dto.setReceiverCurrencyCode(receiverAccount != null ? receiverAccount.getCurrency().getCode() : null);
        
        if (payment.getCard() != null) {
            dto.setCardNumber(payment.getCard().getCardNumber());
        } else {
            dto.setCardNumber(null);
        }

        return dto;
    }
    public PaymentDto toPaymentDto(Payment payment){
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(payment.getId());
        paymentDto.setPaymentPurpose("Tax");
        paymentDto.setPaymentCode("N/A");
        paymentDto.setRecieverName("Republika Srbija");
        paymentDto.setAmount(payment.getAmount());
        paymentDto.setFromAccountNumber(payment.getSenderAccount().getAccountNumber());
        paymentDto.setToAccountNumber(payment.getAccountNumberReceiver());
        paymentDto.setCurrencyCode("RSD");
        return paymentDto;
    }
}