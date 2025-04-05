package rs.raf.stock_service.domain.entity;

import lombok.*;
import rs.raf.stock_service.domain.enums.OptionType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("OPTION")
@Builder
public class Option extends Listing {
    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    private BigDecimal strikePrice;
    private BigDecimal impliedVolatility;
    private BigDecimal contractSize;
    private Integer openInterest;
    private LocalDate settlementDate;
    private BigDecimal maintenanceMargin;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock underlyingStock;

    @ManyToOne
    @JoinColumn(name = "otc_offer_id", nullable = true)
    private OtcOffer offer;
}
