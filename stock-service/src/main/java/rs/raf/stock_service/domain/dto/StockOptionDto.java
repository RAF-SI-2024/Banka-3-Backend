package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class StockOptionDto {
    private BigDecimal strikePrice;
    private BigDecimal impliedVolatility;
    private Integer openInterest;
    private String optionType;

    public StockOptionDto() {}

    public StockOptionDto(BigDecimal strikePrice, BigDecimal impliedVolatility, Integer openInterest, String optionType) {
        this.strikePrice = strikePrice;
        this.impliedVolatility = impliedVolatility;
        this.openInterest = openInterest;
        this.optionType = optionType;
    }

}
