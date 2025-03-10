package rs.raf.bank_service.domain.dto;

import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ExchangeRateDto {

    @NotBlank(message = "From currency is required")
    private String fromCurrency;

    @NotBlank(message = "To currency is required")
    private String toCurrency;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.0001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRate;
}
