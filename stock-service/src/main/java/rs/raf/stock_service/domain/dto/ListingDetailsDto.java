package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import rs.raf.stock_service.domain.enums.ListingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ListingDetailsDto {
    private Long id;
    private ListingType listingType;
    private String ticker;
    private String name;
    private BigDecimal currentPrice;
    private String exchangeMic;
    private List<PriceHistoryDto> priceHistory;
    private Integer contractSize;
    private String contractUnit;
    private List<LocalDate> optionSettlementDates;

    public ListingDetailsDto() {}

    public ListingDetailsDto(Long id, ListingType listingType, String ticker, String name, BigDecimal currentPrice, String exchangeMic,
                             List<PriceHistoryDto> priceHistory, Integer contractSize, String contractUnit,List<LocalDate> optionSettlementDates) {
        this.id = id;
        this.listingType = listingType;
        this.ticker = ticker;
        this.name = name;
        this.currentPrice = currentPrice;
        this.exchangeMic = exchangeMic;
        this.priceHistory = priceHistory;
        this.contractSize = contractSize;
        this.contractUnit = contractUnit;
        this.optionSettlementDates = optionSettlementDates;
    }

}
