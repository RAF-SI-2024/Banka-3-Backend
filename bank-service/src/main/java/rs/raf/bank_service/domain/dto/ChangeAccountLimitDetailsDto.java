package rs.raf.bank_service.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ChangeAccountLimitDetailsDto {
    private String accountNumber;
    private BigDecimal oldLimit;
    private BigDecimal newLimit;
}
