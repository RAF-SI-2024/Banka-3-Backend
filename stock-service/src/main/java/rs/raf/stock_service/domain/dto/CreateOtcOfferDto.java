package rs.raf.stock_service.domain.dto;

import lombok.*;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOtcOfferDto {

    @NotNull
    private Long portfolioEntryId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal pricePerStock;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal premium;

    @NotNull
    @Future
    private LocalDate settlementDate;

}
