package rs.raf.stock_service.domain.dto;

import lombok.*;
import rs.raf.stock_service.domain.enums.OtcOptionStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtcOptionDto {
    private Long id;
    private String stockSymbol;
    private Integer amount;
    private BigDecimal strikePrice;
    private BigDecimal premium;
    private String settlementDate;
    private String sellerInfo;
    private BigDecimal profit;
    private OtcOptionStatus status; // opcija se moze iskoristiti ako je status == VALID i ako je used == false
    private boolean used;
}