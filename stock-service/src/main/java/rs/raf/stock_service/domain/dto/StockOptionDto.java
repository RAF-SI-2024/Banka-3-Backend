package rs.raf.stock_service.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class StockOptionDto {
    private BigDecimal strikePrice;
    private BigDecimal impliedVolatility;
    private Integer openInterest;
    private String optionType;
    private BigDecimal premium;
    private Long listingId;

    public StockOptionDto() {
    }

    public StockOptionDto(BigDecimal strikePrice, BigDecimal impliedVolatility, Integer openInterest, String optionType, BigDecimal premium, Long listingId) {
        this.strikePrice = strikePrice;
        this.impliedVolatility = impliedVolatility;
        this.openInterest = openInterest;
        this.optionType = optionType;
        this.premium = premium;
        this.listingId = listingId;
    }
}