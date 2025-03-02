package rs.raf.bank_service.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;

import javax.persistence.*;
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
    private String type;
    private String name;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private String accountNumber;
    private String status;
    private BigDecimal cardLimit;
}
