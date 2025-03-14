package rs.raf.stock_service.domain.entity;

import lombok.*;

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
    private Long outstandingShares;
    private BigDecimal dividendYield;
}