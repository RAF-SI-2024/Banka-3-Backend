package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("FUTURES")
public class FuturesContract extends Listing {
    private Integer contractSize;
    private String ticker;
    private BigDecimal maintenanceMargin;
    private String contractUnit;
    private LocalDate settlementDate;
}