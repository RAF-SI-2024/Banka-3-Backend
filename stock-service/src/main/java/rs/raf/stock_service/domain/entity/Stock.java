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
@DiscriminatorValue("STOCK")
public class Stock extends Listing {
    private String symbol;
    private BigDecimal change;
    private long volume;
    private long outstandingShares;
    private BigDecimal dividendYield;
    private BigDecimal marketCap;
    private int contractSize = 1;
    private BigDecimal maintenanceMargin;
}