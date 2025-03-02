package rs.raf.bank_service.mapper;


import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;

import java.time.LocalDate;

@Component
public class CardMapper {


    public CardDto toCardDto(Card card){
        if (card == null) return null;
        return new CardDto(card.getId(),
                card.getCardNumber(),
                card.getCvv(),
                card.getType().name(),
                card.getName(),
                card.getCreationDate(),
                card.getExpirationDate(),
                card.getAccount().getAccountNumber(),
                card.getStatus().name(),
                card.getCardLimit());
    }

    public static Card toEntity(CardDto dto, Account account) {
        if (dto == null) {
            return null;
        }
        Card card = new Card();
        card.setId(dto.getId());
        card.setCardNumber(dto.getCardNumber());
        card.setCvv(dto.getCvv());
        card.setType(CardType.valueOf(dto.getType()));
        card.setName(dto.getName());
        card.setCreationDate(dto.getCreationDate());
        card.setExpirationDate(dto.getExpirationDate());
        card.setAccount(account);
        card.setStatus(CardStatus.valueOf(dto.getStatus()));
        card.setCardLimit(dto.getCardLimit());

        return card;
    }

    public static Card fromCreateCardDtotoEntity(CreateCardDto dto, Account account) {
        if (dto == null || account == null) {
            return null;
        }
        Card card = new Card();
        card.setCardNumber(dto.getCardNumber());
        card.setCvv(dto.getCvv());
        card.setType(CardType.valueOf(dto.getType()));
        card.setName(dto.getName());
        card.setAccount(account);
        card.setStatus(CardStatus.valueOf(dto.getStatus()));
        card.setCardLimit(dto.getCardLimit());

        return card;
    }
}
