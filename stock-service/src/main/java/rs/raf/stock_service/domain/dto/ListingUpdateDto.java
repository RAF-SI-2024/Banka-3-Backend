package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class ListingUpdateDto {
    private BigDecimal price;
    private BigDecimal ask;


    public ListingUpdateDto(BigDecimal price, BigDecimal ask) {
        this.price = price;
        this.ask = ask;
    }
}
