package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {

    private CurrencyDto fromCurrency;
    private CurrencyDto toCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal sellRate;

    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null || getClass() != object.getClass())
            return false;

        ExchangeRateDto exchangeRateDto = (ExchangeRateDto) object;

        return fromCurrency.equals(exchangeRateDto.getFromCurrency()) && toCurrency.equals(exchangeRateDto.getToCurrency())
                && exchangeRate.equals(exchangeRateDto.getExchangeRate());
    }
}
