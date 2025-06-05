package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListingPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime date;
    @ManyToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    @Column(precision = 20, scale = 6)
    private BigDecimal open;
    @Column(precision = 20, scale = 6)
    private BigDecimal close;
    @Column(precision = 20, scale = 6)
    private BigDecimal high;
    @Column(precision = 20, scale = 6)
    private BigDecimal low;
    @Column(precision = 20, scale = 6)
    private BigDecimal change;
    private Long volume;
}
