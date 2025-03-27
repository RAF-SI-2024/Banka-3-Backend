package rs.raf.bank_service.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class NewBankAccountDto {
    @NotNull(message = "Name cannot be null")
    @Size(min = 3, max = 50, message = "Account name must be between 3 and 50 characters")
    private String name;

    @NotNull(message = "Currency cannot be null")
    private String currency;

    @NotNull(message = "Client ID cannot be null")
    private Long clientId;

    // bice obrisano, vadi se iz jwt
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

    private Long authorizedPersonId;
}
