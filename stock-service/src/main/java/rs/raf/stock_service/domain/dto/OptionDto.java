package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OptionDto {
    private String stockListing;
    private String optionType;// "Call" ili "Put"
    private BigDecimal strikePrice;
    private BigDecimal contractSize;
    private LocalDate settlementDate;
    private BigDecimal maintenanceMargin;
}
