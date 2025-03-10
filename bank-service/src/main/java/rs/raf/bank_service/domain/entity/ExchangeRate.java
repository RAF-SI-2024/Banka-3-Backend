package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "exchange_rate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency_code", "to_currency_code"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id")
    private UUID id;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "from_currency_code")
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "to_currency_code")
    private Currency toCurrency;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    private LocalDate validFrom;
    private LocalDate validTo;
}
