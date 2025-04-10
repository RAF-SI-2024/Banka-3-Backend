package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.stock_service.domain.enums.OptionType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionDto {
    private String stockListing;
    private OptionType optionType;// "Call" ili "Put"
    private BigDecimal strikePrice;
    private BigDecimal contractSize;
    private LocalDate settlementDate;
    private BigDecimal maintenanceMargin;
    private BigDecimal price;
    private String ticker;
    private boolean onSale;
}
