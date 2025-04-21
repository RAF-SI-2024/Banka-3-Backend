package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bank2PaymentConfirmationDto {
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String referenceNumber;
}