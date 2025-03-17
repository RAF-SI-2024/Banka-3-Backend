package rs.raf.bank_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "from_currency_code")
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "to_currency_code")
    private Currency toCurrency;

    @Column(precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(precision = 10, scale = 6)
    private BigDecimal sellRate;

}
