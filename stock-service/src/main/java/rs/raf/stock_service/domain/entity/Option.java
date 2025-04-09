package rs.raf.stock_service.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock underlyingStock;
    
    private boolean onSale;
}
