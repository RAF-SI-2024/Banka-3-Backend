package rs.raf.bank_service.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ChangeAccountLimitDto {

    @NotNull(message = "New limit cannot be null")
    @Positive(message = "Limit must be greater than zero")
    private BigDecimal newLimit;

    @NotNull(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code must be 6 digits")
    private String verificationCode;

    public ChangeAccountLimitDto(BigDecimal newLimit, String verificationCode) {
        this.newLimit = newLimit;
        this.verificationCode = verificationCode;
    }
}
