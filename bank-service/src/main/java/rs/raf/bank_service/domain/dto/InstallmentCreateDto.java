package rs.raf.bank_service.domain.dto;

import lombok.*;
import rs.raf.bank_service.domain.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class InstallmentCreateDto {

    private BigDecimal amount;
    private BigDecimal interestRate;
    private LocalDate expectedDueDate;
    private LocalDate actualDueDate;
    private InstallmentStatus installmentStatus;
    private Long loanId;
}