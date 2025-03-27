package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentDto implements Serializable {

    private BigDecimal amount;
    private String receiverName;
    private String paymentPurpose;
    private String paymentCode;
    private String currencyCode;
    private String fromAccountNumber;
    private String toAccountNumber;

}
