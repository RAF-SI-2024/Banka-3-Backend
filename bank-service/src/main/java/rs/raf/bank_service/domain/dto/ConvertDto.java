package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDto {

    @NotNull(message = "From currency cannot be null")
    private String fromCurrencyCode;

    @NotNull(message = "To currency cannot be null")
    private String toCurrencyCode;

    @NotNull(message = "Amount cannot be null")
    private BigDecimal amount;
}
