package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("FOREX")
public class ForexPair extends Listing {
    private String baseCurrency;
    private String quoteCurrency;
    @Column(precision = 20, scale = 6)
    private BigDecimal exchangeRate;
    private String liquidity; // npr. "High", "Medium", "Low" – ovde možemo podrazumevati "Medium"
    private int contractSize = 1000; // standardno 1000
    @Column(precision = 20, scale = 6)
    private BigDecimal maintenanceMargin; // = contractSize * exchangeRate * 0.10
    private BigDecimal nominalValue;
}