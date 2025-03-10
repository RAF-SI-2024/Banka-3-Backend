package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.CurrencyDto;
import rs.raf.bank_service.domain.entity.Currency;

import java.math.BigDecimal;

public class CurrencyMapper {

    // ✅ Mapiranje iz Currency u CurrencyDto (bez polja active)
    public static CurrencyDto toDto(Currency currency) {
        if (currency == null) return null;
        return new CurrencyDto(
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.getCountries(),
                currency.getDescription(),
                currency.isActive()
        );
    }
}
