package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ForexPairDto {
    private String name;
    private String ticker;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal exchangeRate;
    private String liquidity;
    private LocalDateTime lastRefresh;
    private BigDecimal maintenanceMargin;
    private BigDecimal nominalValue;
    private BigDecimal ask;
    private BigDecimal price;
}
