package rs.raf.stock_service.domain.entity;

import lombok.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;
import rs.raf.stock_service.domain.enums.OtcOptionStatus;

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

    private Long buyerId;
    private Long sellerId;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock underlyingStock;

    private Integer amount;
    private BigDecimal strikePrice;
    private LocalDate settlementDate;
    private BigDecimal premium;

    @Enumerated(EnumType.STRING)
    private OtcOptionStatus status;

    @OneToOne(mappedBy = "otcOption", cascade = CascadeType.ALL)
    private OtcOffer otcOffer;
}
