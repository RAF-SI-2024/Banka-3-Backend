package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
@Getter
@Setter
public class ListingDetailsDto {
    private Long id;
    private String ticker;
    private String name;
    private BigDecimal currentPrice;
    private String exchangeMic;
    private List<PriceHistoryDto> priceHistory;
    private Integer contractSize;
    private String contractUnit;

    public ListingDetailsDto() {}

    public ListingDetailsDto(Long id, String ticker, String name, BigDecimal currentPrice, String exchangeMic,
                             List<PriceHistoryDto> priceHistory, Integer contractSize, String contractUnit) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.currentPrice = currentPrice;
        this.exchangeMic = exchangeMic;
        this.priceHistory = priceHistory;
        this.contractSize = contractSize;
        this.contractUnit = contractUnit;
    }

}
