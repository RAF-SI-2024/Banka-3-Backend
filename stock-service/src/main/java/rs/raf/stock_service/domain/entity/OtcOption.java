package rs.raf.stock_service.domain.entity;

import lombok.*;
import rs.raf.stock_service.domain.enums.OptionType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtcOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal strikePrice;
    private LocalDate settlementDate;
    private Integer amount;

    private Long buyerId;
    private Long sellerId;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock underlyingStock;

    private boolean used;

    @OneToOne(mappedBy = "otcOption", cascade = CascadeType.ALL)
    private OtcOffer otcOffer;
}
