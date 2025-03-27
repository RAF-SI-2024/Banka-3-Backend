package rs.raf.bank_service.domain.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.domain.enums.RequestStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    private CardIssuer cardIssuer;

    private BigDecimal cardLimit;

    private String name;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
