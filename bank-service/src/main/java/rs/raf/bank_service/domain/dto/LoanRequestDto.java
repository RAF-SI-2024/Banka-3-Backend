package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.EmploymentStatus;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDto {

    private LoanType type;
    private BigDecimal amount;
    private String purpose;
    private BigDecimal monthlyIncome;
    private EmploymentStatus employmentStatus;
    private Integer employmentDuration;
    private Integer repaymentPeriod;
    private String contactPhone;
    private String accountNumber;
    private String currencyCode;
    private LoanRequestStatus status;
}
