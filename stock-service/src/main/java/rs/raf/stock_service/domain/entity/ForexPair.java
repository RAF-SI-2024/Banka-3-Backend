package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
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
    private BigDecimal exchangeRate;
    private String liquidity; // npr. "High", "Medium", "Low" – ovde možemo podrazumevati "Medium"
    private int contractSize = 1000; // standardno 1000
    private BigDecimal maintenanceMargin; // = contractSize * exchangeRate * 0.10
    private BigDecimal nominalValue;
}