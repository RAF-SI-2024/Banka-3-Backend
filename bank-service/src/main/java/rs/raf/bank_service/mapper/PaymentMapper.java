package rs.raf.bank_service.mapper;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.entity.Payment;

@Component
public class PaymentMapper {

    public PaymentOverviewDto toOverviewDto(Payment payment) {
        PaymentOverviewDto dto = new PaymentOverviewDto();
        dto.setId(payment.getId());
        dto.setSenderName(payment.getSenderName());
        dto.setAmount(payment.getAmount());
        dto.setDate(payment.getDate());
        dto.setStatus(payment.getStatus());
        return dto;
    }

    public PaymentDetailsDto toDetailsDto(Payment payment) {
        PaymentDetailsDto dto = new PaymentDetailsDto();
        dto.setId(payment.getId());
        dto.setSenderName(payment.getSenderName());
        dto.setAmount(payment.getAmount());
        dto.setAccountNumberReceiver(payment.getAccountNumberReciver());
        dto.setPaymentCode(payment.getPaymentCode());
        dto.setPurposeOfPayment(payment.getPurposeOfPayment());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setDate(payment.getDate());
        dto.setStatus(payment.getStatus());
        return dto;
    }
}