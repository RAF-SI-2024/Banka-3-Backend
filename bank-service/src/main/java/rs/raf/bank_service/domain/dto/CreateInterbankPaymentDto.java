package rs.raf.bank_service.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreateInterbankPaymentDto {
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String paymentCode;
    private String purposeOfPayment;
    private String referenceNumber;
}
