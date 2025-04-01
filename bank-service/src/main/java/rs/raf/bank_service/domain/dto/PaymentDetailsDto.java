package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDetailsDto {
    private Long id;
    private String senderName;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private String accountNumberReceiver;
    private String paymentCode;
    private String purposeOfPayment;
    private String referenceNumber;
    private LocalDateTime date;
    private PaymentStatus status;
    private String cardNumber;
    private String senderCurrencyCode;
    private String receiverCurrencyCode;
}