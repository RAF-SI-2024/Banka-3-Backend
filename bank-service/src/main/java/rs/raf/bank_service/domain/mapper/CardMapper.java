package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CardDtoNoOwner;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;

public class CardMapper {

    public static CardDto toDto(Card card, ClientDto client) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setCardNumber(card.getCardNumber());
        dto.setCvv(card.getCvv());
        dto.setCreationDate(card.getCreationDate());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setStatus(card.getStatus());
        dto.setCardLimit(card.getCardLimit());
        dto.setOwner(client);
        dto.setType(card.getType());
        dto.setIssuer(card.getIssuer());
        dto.setName(card.getName());
        dto.setAccountNumber(card.getAccount().getAccountNumber());
        return dto;
    }

    public static CardDtoNoOwner toCardDtoNoOwner(Card card) {
        if (card == null) return null;
        return new CardDtoNoOwner(
                card.getId(),
                card.getCardNumber(),
                card.getCvv(),
                card.getType(),
                card.getIssuer(),
                card.getName(),
                card.getCreationDate(),
                card.getExpirationDate(),
                card.getAccount().getAccountNumber(),
                card.getStatus(),
                card.getCardLimit());
    }
}