package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String baseCurrencyCode;
    private String quoteCurrencyCode;
    private BigDecimal exchangeRate;

    // mozda ovo bude Long ili Enum ? proveriti da li se dobija nesto sa api
    private String liquidity;
}