package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 16)
    private String cardNumber;

    @Column(length = 3)
    private String cvv;

    @Enumerated(EnumType.STRING)
    private CardType type;

    private String name;

    private LocalDate creationDate;
    private LocalDate expirationDate;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    private BigDecimal cardLimit;
}