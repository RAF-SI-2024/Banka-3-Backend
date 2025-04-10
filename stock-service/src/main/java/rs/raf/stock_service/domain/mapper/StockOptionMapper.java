package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.dto.StockOptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.entity.Stock;

@Component
public class StockOptionMapper {

    public StockOptionDto toDto(Option option) {
        return new StockOptionDto(
                option.getStrikePrice(),
                option.getImpliedVolatility(),
                option.getOpenInterest(),
                option.getOptionType().name()
        );
    }
}
