package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockDto {
    private String name;
    private String ticker;
    private BigDecimal price;
    private BigDecimal change;
    private long volume;
    private long outstandingShares;
    private BigDecimal dividendYield;
    private BigDecimal marketCap;
    private BigDecimal maintenanceMargin;
    private String exchange;
    private BigDecimal ask;

}
