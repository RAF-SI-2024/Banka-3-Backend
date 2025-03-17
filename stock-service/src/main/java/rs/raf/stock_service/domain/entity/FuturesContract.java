package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("FUTURES")
public class FuturesContract extends Listing {
    private Integer contractSize;

    // mozda bude enum ? zavisi sta ima na apiju / podacima
    private String contractUnit;
    private LocalDate settlementDate;
}