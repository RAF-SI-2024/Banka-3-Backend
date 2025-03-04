package rs.raf.bank_service.unit;

import feign.FeignException;
import feign.Request;
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
import rs.raf.bank_service.domain.entity.CompanyAccount;
import rs.raf.bank_service.domain.enums.AccountOwnerType;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.CardLimitExceededException;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.InvalidTokenException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;
import rs.raf.bank_service.service.CardService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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


    @Test
    public void testCreateCard_Success() {
        // Prepare mock data
        String accountNumber = "account123";
        CreateCardDto createCardDto = new CreateCardDto("DEBIT", "Visa", accountNumber, new BigDecimal("1000.00"));
        Account account = mock(Account.class); // Mocked Account object
        account.setAccountNumber(accountNumber);

        when(accountRepository.findByAccountNumber(createCardDto.getAccountNumber())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto("account123", "Personal"));
        when(cardRepository.countByAccount(account)).thenReturn(0L); // No cards for the account

        // Call the service method
        CardDtoNoOwner result = cardService.createCard(createCardDto);

        // Verify results
        assertNotNull(result);
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    public void testCreateCard_AccountNotFound() {
        // Prepare mock data
        CreateCardDto createCardDto = new CreateCardDto("invalidAccount", "Visa", "John Doe", new BigDecimal("1000.00"));

        when(accountRepository.findByAccountNumber(createCardDto.getAccountNumber())).thenReturn(Optional.empty());

        // Assert exception is thrown
        assertThrows(EntityNotFoundException.class, () -> cardService.createCard(createCardDto));
    }

    @Test
    public void testCreateCard_CardLimitExceeded() {
        // Prepare mock data
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));
        Account account = mock(Account.class);
        account.setAccountNumber("account123");

        when(accountRepository.findByAccountNumber(createCardDto.getAccountNumber())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto("account123", "Personal"));
        when(cardRepository.countByAccount(account)).thenReturn(2L); // Account already has 2 cards

        // Assert exception is thrown
        assertThrows(CardLimitExceededException.class, () -> cardService.createCard(createCardDto));
    }


    @Test
    public void testRequestCardForAccount_AccountNotFound() {
        // Prepare mock data
        CreateCardDto createCardDto = new CreateCardDto("invalidAccount", "Visa", "John Doe", new BigDecimal("1000.00"));

        when(accountRepository.findByAccountNumber(createCardDto.getAccountNumber())).thenReturn(Optional.empty());

        // Assert exception is thrown
        assertThrows(EntityNotFoundException.class, () -> cardService.requestCardForAccount(createCardDto));
    }


    @Test
    public void testRequestCardForAccount_Success() {
        // Prepare mock data
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));
        Account account = mock(Account.class);
        account.setAccountNumber("account123");

        when(accountRepository.findByAccountNumber(createCardDto.getAccountNumber())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto("account123", "Personal"));
        when(cardRepository.countByAccount(account)).thenReturn(0L);
        when(userClient.getClientById(anyLong())).thenReturn(new ClientDto(1234L,"John","Doe","test@example.com"));
        doNothing().when(userClient).requestCard(any(RequestCardDto.class));

        // Call the service method
        cardService.requestCardForAccount(createCardDto);

        // Verify interactions
        verify(userClient, times(1)).requestCard(any(RequestCardDto.class));
    }

    @Test
    public void testGetUserCards_Success() {
        // Arrange
        List<Account> userAccounts = Arrays.asList(dummyAccount);
        dummyAccount.getCards().add(dummyCard);

        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
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
        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(accountRepository.findByClientId(1L)).thenReturn(Arrays.asList());

        // Act
        List<CardDto> result = cardService.getUserCards(authHeader);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testBlockCardByUser_Success() {
        // Arrange
        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
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
        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(cardRepository.findByCardNumber("nonExistingCard")).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> cardService.blockCardByUser("nonExistingCard", authHeader));
        assertTrue(exception.getMessage().contains("Card not found"));
    }

    @Test
    public void testBlockCardByUser_UnauthorizedUser() {
        // Arrange
        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(2L); // Different user ID
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber())).thenReturn(Optional.of(dummyCard));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> cardService.blockCardByUser(dummyCard.getCardNumber(), authHeader));
        assertEquals("You can only block your own cards", exception.getMessage());
    }
}