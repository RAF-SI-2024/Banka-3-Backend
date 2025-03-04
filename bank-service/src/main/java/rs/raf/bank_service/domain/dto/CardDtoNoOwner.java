package rs.raf.bank_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardDtoNoOwner {

    private Long id;
    private String cardNumber;
    private String cvv;
    private String type;
    private String name;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private String accountNumber;
    private CardStatus status;
    private BigDecimal cardLimit;
}
