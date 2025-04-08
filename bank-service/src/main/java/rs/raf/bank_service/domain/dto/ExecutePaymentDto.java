package rs.raf.bank_service.domain.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@Data
public class ExecutePaymentDto {
    private Long clientId;
    private Long senderAccountNumber;
    private Long receiverAccountNumber;
    private BigDecimal amount;
    private Integer paymentCode;

}

