package rs.raf.stock_service.domain.dto;

import lombok.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtcOfferDto {
    private Long id;
    private StockDto stock;
    private Integer amount;
    private BigDecimal pricePerStock;
    private BigDecimal premium;
    private LocalDate settlementDate;
    private OtcOfferStatus status;

}
