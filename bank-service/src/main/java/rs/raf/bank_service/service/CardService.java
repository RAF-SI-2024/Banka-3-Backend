package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.dto.CardDto;
import rs.raf.bank_service.mapper.CardMapper;
import rs.raf.bank_service.repository.CardRepository;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public List<CardDto> getClientCards(Long clientId) {
        return cardRepository.findByAccountClientId(clientId).stream()
                .map(cardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CardDto blockCard(String cardNumber, Long clientId) {
        Card card = cardRepository.findByCardNumberAndAccountClientId(cardNumber, clientId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found or doesn't belong to client"));

        if (card.getStatus() == CardStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot block a deactivated card");
        }

        card.setStatus(CardStatus.BLOCKED);
        return cardMapper.toDto(cardRepository.save(card));
    }

    @Transactional
    public CardDto unblockCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        if (card.getStatus() == CardStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot unblock a deactivated card");
        }

        card.setStatus(CardStatus.ACTIVE);
        return cardMapper.toDto(cardRepository.save(card));
    }

    @Transactional
    public CardDto deactivateCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));

        card.setStatus(CardStatus.DEACTIVATED);
        return cardMapper.toDto(cardRepository.save(card));
    }
}