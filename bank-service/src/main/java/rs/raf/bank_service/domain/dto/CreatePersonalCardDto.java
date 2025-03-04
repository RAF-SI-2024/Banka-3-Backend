package rs.raf.bank_service.domain.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class CreatePersonalCardDto {
    @NotBlank(message = "Account number cannot be empty")
    @Pattern(regexp = "\\d{18}", message = "Account number must be exactly 18 digits")
    private String accountNumber;

    @NotNull(message = "Card limit cannot be null")
    @Positive(message = "Card limit must be greater than zero")
    @DecimalMax(value = "1000000.00", message = "Card limit cannot exceed 1,000,000")
    @Digits(integer = 7, fraction = 2, message = "Card limit must have at most 7 digits before decimal and 2 after")
    private BigDecimal cardLimit;
}