package rs.raf.bank_service.domain.entity;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "futures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuturesContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Npr. CLJ22, SIH23...
    @Column(nullable = false, unique = true)
    private String futuresSymbol;

    // Proizvod
    @Column(nullable = false)
    private String underlyingProduct;

    // Datum isteka
    private LocalDate settlementDate;

    // Kontrakt veličina
    private Integer contractSize;

    // Jedinica mere (barel, kg, ...)
    private String contractUnit;

    // Cena fucersa (poslednja dobijena)
    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    // MaintenanceMargin = ContractSize * Price * 0.1, i sl.
    @Column(precision = 15, scale = 2)
    private BigDecimal maintenanceMargin;
}
