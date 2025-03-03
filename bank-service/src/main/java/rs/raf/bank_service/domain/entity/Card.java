package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardStatus;

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

    private String cvv;
    private LocalDate creationDate;
    private LocalDate expirationDate;

    @ManyToOne
    private Account account;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    private BigDecimal cardLimit;
}