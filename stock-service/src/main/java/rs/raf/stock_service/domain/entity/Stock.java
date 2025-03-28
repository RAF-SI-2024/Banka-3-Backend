package rs.raf.stock_service.domain.entity;

import lombok.*;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;


@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("STOCK")
public class Stock extends Listing {
    private BigDecimal change;
    private long volume;
    private long outstandingShares;
    private BigDecimal dividendYield;
    private BigDecimal marketCap;
    private int contractSize = 1;
    private BigDecimal maintenanceMargin;
}
