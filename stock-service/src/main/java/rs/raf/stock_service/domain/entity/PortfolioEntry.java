package rs.raf.stock_service.domain.entity;

import lombok.*;
import rs.raf.stock_service.domain.enums.ListingType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(optional = false)
    private Listing listing;

    @Enumerated(EnumType.STRING)
    private ListingType type;

    @Column(nullable = false)
    private Integer amount;

    private Integer publicAmount = 0;

    private Integer reservedAmount = 0;

    @Column(precision = 10, scale = 6)
    private BigDecimal averagePrice;

    private Boolean inTheMoney = false;    // Za opcije

    private Boolean used = false;          // Za opcije

    private LocalDateTime lastModified;

    // moze se npr i dodati i currentProfit, ali bolje ga izračunavati na getPortfolio pozivu

    //Za order i Option (ne otc) prodaju je slobodno samo sto nije public i rezervisano
    public Integer getAvailableAmount(){
        return amount - publicAmount - reservedAmount;
    }
}
