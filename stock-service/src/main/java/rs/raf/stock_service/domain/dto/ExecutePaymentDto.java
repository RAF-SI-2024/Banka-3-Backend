package rs.raf.stock_service.domain.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
public class ExecutePaymentDto {
    private Long clientId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String purposeOfPayment;
    private String paymentCode;
    private String referenceNumber;
}
