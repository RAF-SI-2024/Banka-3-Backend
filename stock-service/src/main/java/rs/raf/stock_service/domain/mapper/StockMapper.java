package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.entity.Stock;

@Component
public class StockMapper {

    public StockDto toDto(Stock stock) {
        return StockDto.builder()
                .name(stock.getName())
                .ticker(stock.getTicker())
                .price(stock.getPrice())
                .change(stock.getChange())
                .volume(stock.getVolume())
                .outstandingShares(stock.getOutstandingShares())
                .dividendYield(stock.getDividendYield())
                .marketCap(stock.getMarketCap())
                .maintenanceMargin(stock.getMaintenanceMargin())
                .exchange(stock.getExchange().getName()) // Ako je `exchange` entitet
                .build();
    }
}
