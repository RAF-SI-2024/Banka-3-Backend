package rs.raf.stock_service.domain.entity;


import lombok.*;
import rs.raf.stock_service.domain.enums.OtcOfferStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtcOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;

    private Long buyerId;
    private Long sellerId;

    private Integer amount;
    private BigDecimal pricePerStock;
    private BigDecimal premium;
    private LocalDate settlementDate;

    private LocalDateTime lastModified;
    private Long lastModifiedById;

    @Enumerated(EnumType.STRING)
    private OtcOfferStatus status;

    @OneToOne
    private Option option;

}
