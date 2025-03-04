package rs.raf.bank_service.domain.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class CreateCompanyCardDto {
    @NotBlank(message = "Account number cannot be empty")
    @Pattern(regexp = "\\d{18}", message = "Account number must be exactly 18 digits")
    private String accountNumber;

    @NotNull(message = "Card limit cannot be null")
    @Positive(message = "Card limit must be greater than zero")
    @DecimalMax(value = "10000000.00", message = "Card limit cannot exceed 10,000,000")
    @Digits(integer = 8, fraction = 2, message = "Card limit must have at most 8 digits before decimal and 2 after")
    private BigDecimal cardLimit;

    @Positive(message = "Authorized personnel ID must be a positive number")
    private Long authorizedPersonnelId;

    @AssertTrue(message = "Either isForOwner must be true or authorizedPersonnelId must be provided")
    private boolean isValidCardHolder() {
        return isForOwner || authorizedPersonnelId != null;
    }

    private boolean isForOwner = false;
}