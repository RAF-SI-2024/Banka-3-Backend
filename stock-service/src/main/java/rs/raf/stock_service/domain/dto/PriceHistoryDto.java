package rs.raf.stock_service.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PriceHistoryDto {
    private LocalDate date;
    private BigDecimal price;

    public PriceHistoryDto() {
    }

    public PriceHistoryDto(LocalDate date, BigDecimal price) {
        this.date = date;
        this.price = price;
    }
}
