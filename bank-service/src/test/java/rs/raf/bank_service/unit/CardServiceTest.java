package rs.raf.bank_service.unit;

import feign.FeignException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.entity.Card;
import rs.raf.bank_service.domain.entity.PersonalAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenUtil jwtTokenUtil;  // Mocked JwtTokenUtil

    @Mock
    private Claims claims;

    @InjectMocks
    private CardService cardService;

    private Card dummyCard;
    private Account dummyAccount;
    private ClientDto dummyClient;
    private String authHeader;

    @BeforeEach
    public void setUp() {
        dummyAccount = new Account() {
        };
        dummyAccount.setAccountNumber("123456789012345678");
        dummyAccount.setClientId(1L);
        dummyAccount.setAccountOwnerType(AccountOwnerType.PERSONAL);

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

        authHeader = "Bearer dummy-token";

        // Mock JwtTokenUtil behavior
        lenient().when(jwtTokenUtil.getUserIdFromAuthHeader(anyString())).thenReturn(1L);
    }

    @Test
    public void testGetUserCards_Success() {
        // Arrange
        List<Account> userAccounts = Arrays.asList(dummyAccount);
        dummyAccount.getCards().add(dummyCard);

        when(accountRepository.findByClientId(1L)).thenReturn(userAccounts);
        when(userClient.getClientById(1L)).thenReturn(dummyClient);

        // Act
        List<CardDto> result = cardService.getUserCards(authHeader);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1111222233334444", result.get(0).getCardNumber());
        assertEquals("Petar", result.get(0).getOwner().getFirstName());
    }

    @Test
    public void testGetUserCards_NoAccounts() {
        // Arrange
        when(accountRepository.findByClientId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<CardDto> result = cardService.getUserCards(authHeader);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBlockCardByUser_Success() {
        // Arrange
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber())).thenReturn(Optional.of(dummyCard));
        when(userClient.getClientById(dummyAccount.getClientId())).thenReturn(dummyClient);

        // Act
        cardService.blockCardByUser(dummyCard.getCardNumber(), authHeader);

        // Assert
        assertEquals(CardStatus.BLOCKED, dummyCard.getStatus());
        verify(cardRepository).save(dummyCard);
        verify(rabbitTemplate).convertAndSend(eq("card-status-change"), any(EmailRequestDto.class));
    }

    @Test
    public void testBlockCardByUser_CardNotFound() {
        // Arrange
        when(cardRepository.findByCardNumber("nonExistingCard")).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> cardService.blockCardByUser("nonExistingCard", authHeader));
        assertTrue(exception.getMessage().contains("Card not found"));
    }

    @Test
    public void testBlockCardByUser_UnauthorizedUser() {
        // Arrange
        when(jwtTokenUtil.getUserIdFromAuthHeader(anyString())).thenReturn(2L); // Different user ID
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber())).thenReturn(Optional.of(dummyCard));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> cardService.blockCardByUser(dummyCard.getCardNumber(), authHeader));
        assertEquals("You can only block your own cards", exception.getMessage());
    }


    @Test
    public void testCreateCard_Success() {
        // Arrange
        CreateCardDto createCardDto = new CreateCardDto(
                CardType.CREDIT.name(),                // sada CAPS
                "visa",
                dummyAccount.getAccountNumber(),
                BigDecimal.valueOf(1000)
        );

        when(accountRepository.findByAccountNumber(dummyAccount.getAccountNumber()))
                .thenReturn(Optional.of(dummyAccount));
        when(accountMapper.toAccountTypeDto(dummyAccount))
                .thenReturn(new AccountTypeDto("123456789012345678", AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(dummyAccount))
                .thenReturn(0L);

        // Act
        CardDtoNoOwner result = cardService.createCard(createCardDto);

        // Assert
        assertNotNull(result);
        assertEquals(createCardDto.getName(), result.getName());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    public void testCreateCard_CardLimitExceeded() {
        // Arrange
        CreateCardDto createCardDto = new CreateCardDto(
                CardType.CREDIT.name(),                // sada CAPS
                "visa",
                dummyAccount.getAccountNumber(),
                BigDecimal.valueOf(1000)
        );

        when(accountRepository.findByAccountNumber(dummyAccount.getAccountNumber())).thenReturn(Optional.of(dummyAccount));
        when(accountMapper.toAccountTypeDto(dummyAccount)).thenReturn(new AccountTypeDto("123", AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(dummyAccount)).thenReturn(2L);

        // Act & Assert
        assertThrows(CardLimitExceededException.class, () -> cardService.createCard(createCardDto));
    }

    @Test
    public void testRequestCardForAccount_ClientNotFound() {
        // Arrange
        CreateCardDto createCardDto = new CreateCardDto();
        createCardDto.setAccountNumber(dummyAccount.getAccountNumber());

        when(accountRepository.findByAccountNumber(dummyAccount.getAccountNumber())).thenReturn(Optional.of(dummyAccount));
        when(accountMapper.toAccountTypeDto(dummyAccount)).thenReturn(new AccountTypeDto("123", AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(dummyAccount)).thenReturn(0L);

        // FeignException.NotFound simulacija
        when(userClient.getClientById(dummyAccount.getClientId()))
                .thenThrow(FeignException.NotFound.class);

        // Act & Assert
        assertThrows(ClientNotFoundException.class, () -> cardService.requestCardForAccount(createCardDto));
    }

    @Test
    public void testReceiveCardForAccount_InvalidToken() {
        // Arrange
        CreateCardDto createCardDto = new CreateCardDto();
        String token = "invalid-token";

        doThrow(FeignException.NotFound.class)
                .when(userClient).checkToken(any(CheckTokenDto.class));

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> cardService.recieveCardForAccount(token, createCardDto));
    }

    @Test
    public void testChangeCardStatus_Success() {
        // Arrange
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber())).thenReturn(Optional.of(dummyCard));
        when(userClient.getClientById(dummyAccount.getClientId())).thenReturn(dummyClient);

        // Act
        cardService.changeCardStatus(dummyCard.getCardNumber(), CardStatus.BLOCKED);

        // Assert
        assertEquals(CardStatus.BLOCKED, dummyCard.getStatus());
        verify(cardRepository).save(dummyCard);
        verify(rabbitTemplate).convertAndSend(eq("card-status-change"), any(EmailRequestDto.class));
    }

    @Test
    void testGetUserCardsForAccount_Success() {
        String accountNumber = "12345";
        String authHeader = "Bearer token";
        Long clientId = 1L;

        Account account = new PersonalAccount();
        account.setAccountNumber(accountNumber);

        Card card1 = new Card();
        card1.setAccount(account);

        Card card2 = new Card();
        card2.setAccount(account);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(accountRepository.findByAccountNumberAndClientId(accountNumber, clientId))
                .thenReturn(Optional.of(new PersonalAccount()));
        when(cardRepository.findByAccount_AccountNumber(accountNumber))
                .thenReturn(List.of(card1, card2));
        when(userClient.getClientById(clientId)).thenReturn(new ClientDto());

        List<CardDto> cards = cardService.getUserCardsForAccount(accountNumber, authHeader);

        assertEquals(2, cards.size());
    }

    @Test
    void testGetUserCardsForAccount_AccountNotFound() {
        String accountNumber = "12345";
        String authHeader = "Bearer token";
        Long clientId = 1L;

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(clientId);
        when(accountRepository.findByAccountNumberAndClientId(accountNumber, clientId))
                .thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> cardService.getUserCardsForAccount(accountNumber, authHeader));
    }

}