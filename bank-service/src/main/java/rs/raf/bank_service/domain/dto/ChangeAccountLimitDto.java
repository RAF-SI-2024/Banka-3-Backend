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

    public ChangeAccountLimitDto(BigDecimal newLimit) {
        this.newLimit = newLimit;

    }
}
