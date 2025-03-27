package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Option extends Listing {
    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    private BigDecimal strikePrice;
    private BigDecimal impliedVolatility;
    private Integer openInterest;
    private LocalDate settlementDate;

    @ManyToOne
    private Stock underlyingStock;
}