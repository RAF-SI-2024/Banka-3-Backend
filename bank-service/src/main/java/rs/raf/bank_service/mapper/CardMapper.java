package rs.raf.bank_service.mapper;

import org.springframework.stereotype.Component;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.dto.CardDto;

@Component
public class CardMapper {

    public CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setCardNumber(card.getCardNumber());
        dto.setCvv(card.getCvv());
        dto.setCreationDate(card.getCreationDate());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setAccountId(card.getAccount() != null ? card.getAccount().getClientId() : null);
        dto.setStatus(card.getStatus());
        dto.setCardLimit(card.getCardLimit());
        return dto;
    }
}