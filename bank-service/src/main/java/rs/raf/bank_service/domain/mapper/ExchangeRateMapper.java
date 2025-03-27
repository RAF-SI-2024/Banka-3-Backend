package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.entity.ExchangeRate;

public class ExchangeRateMapper {

    // ✅ Mapiranje iz ExchangeRate u ExchangeRateDto
    public static ExchangeRateDto toDto(ExchangeRate exchangeRate) {
        if (exchangeRate == null) return null;
        return new ExchangeRateDto(
                CurrencyMapper.toDto(exchangeRate.getFromCurrency()),
                CurrencyMapper.toDto(exchangeRate.getToCurrency()),
                exchangeRate.getExchangeRate(),
                exchangeRate.getSellRate()
        );
    }
}