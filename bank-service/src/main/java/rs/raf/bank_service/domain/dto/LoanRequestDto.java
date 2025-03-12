package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.bank_service.domain.enums.EmploymentStatus;
import rs.raf.bank_service.domain.enums.InterestRateType;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDto {

    @NotNull(message = "Loan type is required")
    private LoanType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", message = "Amount must be at least 0")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    @Size(max = 255, message = "Purpose cannot exceed 255 characters")
    private String purpose;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    @NotNull(message = "Employment duration is required")
    @Min(value = 0, message = "Employment duration cannot be negative")
    private Integer employmentDuration;

    @NotNull(message = "Repayment period is required")
    private Integer repaymentPeriod;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
    private String contactPhone;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{18}$", message = "Invalid account number format")
    private String accountNumber;

    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be in ISO 4217 format (e.g., USD, EUR)")
    private String currencyCode;

    @NotNull(message = "Interest rate type is required")
    private InterestRateType interestRateType;
}
