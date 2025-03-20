package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {

    private Long id;
    private String cardNumber;
    private String cvv;
    private CardType type;
    private CardIssuer issuer;
    private String name;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private String accountNumber;
    private CardStatus status;
    private BigDecimal cardLimit;
    private ClientDto owner;
}
