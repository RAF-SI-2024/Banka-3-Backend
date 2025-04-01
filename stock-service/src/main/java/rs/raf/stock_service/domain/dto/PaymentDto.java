package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentDto implements Serializable {

    private Long id;
    private BigDecimal amount;
    private String paymentPurpose;
    private String fromAccountNumber;
    private String toAccountNumber;

}
