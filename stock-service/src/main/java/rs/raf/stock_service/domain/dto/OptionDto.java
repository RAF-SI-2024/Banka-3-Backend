package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OptionDto {
    private String stockListing;      // npr. "AAPL"
    private String optionType;        // "Call" ili "Put"
    private BigDecimal strikePrice; // sada BigDecimal
    private BigDecimal contractSize;      // obično 100
    private LocalDate settlementDate; // datum isteka opcije
    private BigDecimal maintenanceMargin;
}
