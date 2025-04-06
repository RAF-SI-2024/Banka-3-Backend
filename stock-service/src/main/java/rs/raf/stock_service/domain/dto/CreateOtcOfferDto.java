package rs.raf.stock_service.domain.dto;

import lombok.*;

import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOtcOfferDto {

    private Long portfolioEntryId;

    @DecimalMin("0.01")
    private BigDecimal amount;

    private BigDecimal strikePrice;

    private LocalDate settlementDate;

    private BigDecimal pricePerStock;

    private BigDecimal premium;


}
