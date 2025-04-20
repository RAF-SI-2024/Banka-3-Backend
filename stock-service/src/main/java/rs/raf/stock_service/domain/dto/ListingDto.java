package rs.raf.stock_service.domain.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.stock_service.domain.enums.ListingType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ListingDto {
    private Long id;
    private ListingType listingType;
    private String ticker;
    private BigDecimal price;
    private BigDecimal change;
    private Long volume;
    private BigDecimal initialMarginCost;
    private String exchangeMic;
    private BigDecimal ask;
    private LocalDate settlementDate;

    public ListingDto(Long id, ListingType listingType, String ticker, BigDecimal price, BigDecimal change, Long volume, BigDecimal initialMarginCost, String exchangeMic) {
        this.id = id;
        this.listingType = listingType;
        this.ticker = ticker;
        this.price = price;
        this.change = change;
        this.volume = volume;
        this.initialMarginCost = initialMarginCost;
        this.exchangeMic = exchangeMic;
    }

    public ListingDto(Long id, ListingType listingType, String ticker, BigDecimal price, BigDecimal change, Long volume, BigDecimal initialMarginCost, String exchangeMic, BigDecimal ask, LocalDate settlementDate) {
        this.id = id;
        this.listingType = listingType;
        this.ticker = ticker;
        this.price = price;
        this.change = change;
        this.volume = volume;
        this.initialMarginCost = initialMarginCost;
        this.exchangeMic = exchangeMic;
        this.ask = ask;
        this.settlementDate = settlementDate;
    }

}
