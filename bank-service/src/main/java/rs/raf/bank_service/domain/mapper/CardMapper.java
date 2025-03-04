package rs.raf.bank_service.domain.mapper;

import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.entity.Card;

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
        dto.setAccountNumber(card.getAccount().getAccountNumber());
        return dto;
    }
}