package rs.raf.stock_service.domain.dto;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class ListingDto {
    private Long id;
    private String ticker;
    private BigDecimal price;
    private BigDecimal change;
    private Long volume;
    private BigDecimal initialMarginCost;


    public ListingDto(Long id, String ticker, BigDecimal price, BigDecimal change, Long volume, BigDecimal initialMarginCost) {
        this.id = id;
        this.ticker = ticker;
        this.price = price;
        this.change = change;
        this.volume = volume;
        this.initialMarginCost = initialMarginCost;
    }

}
