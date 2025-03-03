package rs.raf.bank_service.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class NewBankAccountDto {
    @NotNull(message = "Currency cannot be null")
    private String currency;

    @NotNull(message = "Client ID cannot be null")
    private Long clientId;

    @NotNull(message = "Employee ID  cannot be null")
    private Long employeeId;

    private Long companyId;

    @NotNull(message = "Initial balance cannot be null")

    private BigDecimal initialBalance;

    @NotNull(message = "Daily limit cannot be null")
    private BigDecimal dailyLimit;

    @NotNull(message = "Monthly limit cannot be null")
    private BigDecimal monthlyLimit;

    @NotNull(message = "Daily spending cannot be null")
    private BigDecimal dailySpending;

    @NotNull(message = "Monthly spending cannot be null")
    private BigDecimal monthlySpending;

    @NotNull(message = "IsActive field cannot be null")
    private String isActive;

    @NotNull(message = "Account type cannot be null")
    private String accountType;

    @NotNull(message = "AccountOwnerType field cannot be null")
    private String accountOwnerType;

    private boolean createCard;
}
