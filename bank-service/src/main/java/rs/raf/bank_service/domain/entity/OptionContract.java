package rs.raf.bank_service.domain.entity;

import lombok.*;
import rs.raf.bank_service.domain.enums.OptionType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Npr. 'AAPL', 'MSFT', itd.
    @Column(nullable = false)
    private String stockTicker;

    // CALL ili PUT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptionType optionType;

    // Strike price
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal strikePrice;

    // Datum isteka
    private LocalDate expirationDate;

    // Datum poslednje trgovine
    private LocalDate lastTradeDate;

    // Implicirana volatilnost
    @Column(precision = 8, scale = 4)
    private BigDecimal impliedVolatility;

    // Premija (lastPrice)
    @Column(precision = 15, scale = 4)
    private BigDecimal premium;

    // Otvoreni interes
    private Integer openInterest;

    // Označava unikatan simbol, npr. "AAPL230317C00160000"
    @Column(nullable = false, unique = true)
    private String optionSymbol;
}
