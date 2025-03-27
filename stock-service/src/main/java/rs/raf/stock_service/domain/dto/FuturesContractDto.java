package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FuturesContractDto {
    private String ticker;
    private int contractSize;
    private BigDecimal price;
    private String contractUnit;
    private LocalDate settlementDate;
    private BigDecimal maintenanceMargin;
}
