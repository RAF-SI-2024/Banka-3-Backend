package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalPaymentCreateDto {
    private String fromAccountNumber;
    private String toAccountNumber;
    private String fromCurrencyId;
    private String toCurrencyId;
    private BigDecimal amount;
    private String codeId;
    private String referenceNumber;
    private String purpose;
    private Long externalTransactionId;
}
