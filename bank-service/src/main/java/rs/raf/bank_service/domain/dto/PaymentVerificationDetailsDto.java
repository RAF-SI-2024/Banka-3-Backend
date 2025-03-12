package rs.raf.bank_service.domain.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentVerificationDetailsDto {
    String fromAccountNumber;
    String toAccountNumber;
    BigDecimal amount;
}
