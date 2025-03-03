package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.dto.CardDto;
import rs.raf.bank_service.mapper.CardMapper;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.service.CardService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardService cardService;

    private Card testCard;
    private CardDto testCardDto;
    private final Long CLIENT_ID = 1L;
    private final String CARD_NUMBER = "1234567890123456";

    @BeforeEach
    void setUp() {
        // Setup test card
        testCard = new Card();
        testCard.setId(1L);
        testCard.setCardNumber(CARD_NUMBER);
        testCard.setCvv("123");
        testCard.setCreationDate(LocalDate.now());
        testCard.setExpirationDate(LocalDate.now().plusYears(3));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setCardLimit(BigDecimal.valueOf(5000));

        Account account = new Account() {
            // Anonymous implementation of abstract class for testing
        };
        account.setClientId(CLIENT_ID);
        testCard.setAccount(account);

        // Setup test card DTO
        testCardDto = new CardDto();
        testCardDto.setId(1L);
        testCardDto.setCardNumber(CARD_NUMBER);
        testCardDto.setCvv("123");
        testCardDto.setCreationDate(LocalDate.now());
        testCardDto.setExpirationDate(LocalDate.now().plusYears(3));
        testCardDto.setStatus(CardStatus.ACTIVE);
        testCardDto.setCardLimit(BigDecimal.valueOf(5000));
        testCardDto.setAccountId(CLIENT_ID);
    }

    @Test
    void getClientCards_ShouldReturnClientCards() {
        // Arrange
        Card card1 = testCard;
        Card card2 = new Card();
        card2.setId(2L);
        card2.setCardNumber("6543210987654321");
        card2.setStatus(CardStatus.ACTIVE);
        Account account = new Account() {
            // Anonymous implementation of abstract class for testing
        };
        account.setClientId(CLIENT_ID);
        card2.setAccount(account);

        List<Card> cards = Arrays.asList(card1, card2);

        CardDto cardDto1 = testCardDto;
        CardDto cardDto2 = new CardDto();
        cardDto2.setId(2L);
        cardDto2.setCardNumber("6543210987654321");
        cardDto2.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findByAccountClientId(CLIENT_ID)).thenReturn(cards);
        when(cardMapper.toDto(card1)).thenReturn(cardDto1);
        when(cardMapper.toDto(card2)).thenReturn(cardDto2);

        // Act
        List<CardDto> result = cardService.getClientCards(CLIENT_ID);

        // Assert
        assertEquals(2, result.size());
        assertEquals(cardDto1.getCardNumber(), result.get(0).getCardNumber());
        assertEquals(cardDto2.getCardNumber(), result.get(1).getCardNumber());
        verify(cardRepository).findByAccountClientId(CLIENT_ID);
        verify(cardMapper, times(2)).toDto(any(Card.class));
    }

    @Test
    void blockCard_ShouldBlockActiveCard() {
        // Arrange
        when(cardRepository.findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID))
                .thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toDto(testCard)).thenReturn(testCardDto);

        // Act
        CardDto result = cardService.blockCard(CARD_NUMBER, CLIENT_ID);

        // Assert
        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        assertEquals(testCardDto, result);
        verify(cardRepository).findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID);
        verify(cardRepository).save(testCard);
        verify(cardMapper).toDto(testCard);
    }

    @Test
    void blockCard_ShouldThrowException_WhenCardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> cardService.blockCard(CARD_NUMBER, CLIENT_ID));
        verify(cardRepository).findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void blockCard_ShouldThrowException_WhenCardDeactivated() {
        // Arrange
        testCard.setStatus(CardStatus.DEACTIVATED);
        when(cardRepository.findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID))
                .thenReturn(Optional.of(testCard));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> cardService.blockCard(CARD_NUMBER, CLIENT_ID));
        verify(cardRepository).findByCardNumberAndAccountClientId(CARD_NUMBER, CLIENT_ID);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void unblockCard_ShouldUnblockBlockedCard() {
        // Arrange
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toDto(testCard)).thenReturn(testCardDto);

        // Act
        CardDto result = cardService.unblockCard(CARD_NUMBER);

        // Assert
        assertEquals(CardStatus.ACTIVE, testCard.getStatus());
        assertEquals(testCardDto, result);
        verify(cardRepository).findByCardNumber(CARD_NUMBER);
        verify(cardRepository).save(testCard);
        verify(cardMapper).toDto(testCard);
    }

    @Test
    void unblockCard_ShouldThrowException_WhenCardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> cardService.unblockCard(CARD_NUMBER));
        verify(cardRepository).findByCardNumber(CARD_NUMBER);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void unblockCard_ShouldThrowException_WhenCardDeactivated() {
        // Arrange
        testCard.setStatus(CardStatus.DEACTIVATED);
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.of(testCard));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> cardService.unblockCard(CARD_NUMBER));
        verify(cardRepository).findByCardNumber(CARD_NUMBER);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void deactivateCard_ShouldDeactivateCard() {
        // Arrange
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toDto(testCard)).thenReturn(testCardDto);

        // Act
        CardDto result = cardService.deactivateCard(CARD_NUMBER);

        // Assert
        assertEquals(CardStatus.DEACTIVATED, testCard.getStatus());
        assertEquals(testCardDto, result);
        verify(cardRepository).findByCardNumber(CARD_NUMBER);
        verify(cardRepository).save(testCard);
        verify(cardMapper).toDto(testCard);
    }

    @Test
    void deactivateCard_ShouldThrowException_WhenCardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumber(CARD_NUMBER)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> cardService.deactivateCard(CARD_NUMBER));
        verify(cardRepository).findByCardNumber(CARD_NUMBER);
        verify(cardRepository, never()).save(any(Card.class));
    }
}