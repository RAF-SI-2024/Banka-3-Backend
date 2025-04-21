package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterbankPaymentDto {
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currencyCode;
    private String paymentCode;
    private String purpose;
    private String referenceNumber;
}
