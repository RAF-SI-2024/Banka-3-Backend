package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentDto {

    private BigDecimal amount;
    private BigDecimal interestRate;
    private LocalDate expectedDueDate;
    private LocalDate actualDueDate;
    private PaymentStatus paymentStatus;

}