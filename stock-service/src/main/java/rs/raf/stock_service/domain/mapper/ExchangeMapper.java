package rs.raf.stock_service.domain.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.ExchangeDto;
import rs.raf.stock_service.domain.entity.Exchange;

@Component
@AllArgsConstructor
public class ExchangeMapper {

    private final CountryMapper countryMapper;

    public ExchangeDto toDto(Exchange exchange){
        if (exchange == null) return null;

        return new ExchangeDto(
                exchange.getMic(),
                exchange.getName(),
                exchange.getAcronym(),
                countryMapper.toDto(exchange.getPolity()),
                exchange.getCurrencyCode(),
                exchange.getTimeZone(),
                exchange.isTestMode()
        );
    }
}
