package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ListingDailyPriceInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date;
    @ManyToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    private BigDecimal price;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal change;
    private Long volume;
}
