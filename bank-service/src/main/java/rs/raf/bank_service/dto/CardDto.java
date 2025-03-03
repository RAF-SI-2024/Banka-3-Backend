package rs.raf.bank_service.dto;

import lombok.Data;
import rs.raf.bank_service.domain.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardDto {
    private Long id;
    private String cardNumber;
    private String cvv;
    private LocalDate creationDate;
    private LocalDate expirationDate;
    private Long accountId;
    private CardStatus status;
    private BigDecimal cardLimit;
}