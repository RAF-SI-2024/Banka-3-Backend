package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.service.CardService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CardService cardService;

    private Card dummyCard;
    private Account dummyAccount;
    private ClientDto dummyClient;

    @BeforeEach
    public void setUp() {
        dummyAccount = new Account() {
        };
        dummyAccount.setAccountNumber("123456789012345678");
        dummyAccount.setClientId(1L);

        dummyCard = new Card();
        dummyCard.setId(1L);
        dummyCard.setCardNumber("1111222233334444");
        dummyCard.setStatus(CardStatus.ACTIVE);
        dummyCard.setAccount(dummyAccount);
        dummyCard.setCreationDate(LocalDate.now());
        dummyCard.setExpirationDate(LocalDate.now().plusYears(3));
        dummyCard.setCardLimit(BigDecimal.valueOf(1000));

        dummyClient = new ClientDto();
        dummyClient.setId(1L);
        dummyClient.setFirstName("Petar");
        dummyClient.setLastName("Petrovic");
        dummyClient.setEmail("petar@example.com");
    }

    @Test
    public void testGetCardsByAccount() {
        when(cardRepository.findByAccount_AccountNumber("123456789012345678"))
                .thenReturn(Arrays.asList(dummyCard));
        when(userClient.getClientById(1L)).thenReturn(dummyClient);

        List<CardDto> result = cardService.getCardsByAccount("123456789012345678");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1111222233334444", result.get(0).getCardNumber());
        assertEquals("Petar", result.get(0).getOwner().getFirstName());
    }

    @Test
    public void testChangeCardStatus() {
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber()))
                .thenReturn(Optional.of(dummyCard));
        when(userClient.getClientById(dummyAccount.getClientId())).thenReturn(dummyClient);

        cardService.changeCardStatus(dummyCard.getCardNumber(), CardStatus.BLOCKED);

        assertEquals(CardStatus.BLOCKED, dummyCard.getStatus());
        verify(cardRepository, times(1)).save(dummyCard);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq("card-status-change"), any(Object.class));
    }

    @Test
    public void testChangeCardStatus_CardNotFound() {
        when(cardRepository.findByCardNumber("nonExistingCard"))
                .thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> cardService.changeCardStatus("nonExistingCard", CardStatus.DEACTIVATED));
        assertTrue(exception.getMessage().contains("Card not found"));
    }
}