package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDto {
    private String loanNumber;
    private LoanType type;
    private BigDecimal amount;
    private Integer repaymentPeriod;
    private BigDecimal nominalInterestRate;
    private BigDecimal effectiveInterestRate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal nextInstallmentAmount;
    private LocalDate nextInstallmentDate;
    private BigDecimal remainingDebt;
    private String currencyCode;
    private LoanStatus status;
}