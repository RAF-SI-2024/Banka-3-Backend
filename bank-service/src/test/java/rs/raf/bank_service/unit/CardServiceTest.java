package rs.raf.bank_service.unit;

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
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;
import rs.raf.bank_service.service.CardService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private AccountRepository accountRepository;

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
    public void testCreatePersonalCard_Success() {
        // Arrange
        CreatePersonalCardDto createDto = new CreatePersonalCardDto();
        createDto.setAccountNumber(dummyAccount.getAccountNumber());
        createDto.setCardLimit(BigDecimal.valueOf(1000));

        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(dummyAccount));
        when(cardRepository.save(any(Card.class))).thenReturn(dummyCard);
        when(userClient.getClientById(anyLong())).thenReturn(dummyClient);

        // Act
        CardDto result = cardService.createPersonalCard(createDto, authHeader);

        // Assert
        assertNotNull(result);
        assertEquals(dummyCard.getCardNumber(), result.getCardNumber());
        verify(cardRepository).save(any(Card.class));
        verify(rabbitTemplate).convertAndSend(eq("card-creation"), any(EmailRequestDto.class));
    }

    @Test
    public void testCreateCompanyCard_Success() {
        // Arrange
        CreateCompanyCardDto createDto = new CreateCompanyCardDto();
        createDto.setAccountNumber(dummyAccount.getAccountNumber());
        createDto.setCardLimit(BigDecimal.valueOf(5000));
        createDto.setForOwner(true);

        // Create a proper CompanyAccount for this test
        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setAccountNumber(dummyAccount.getAccountNumber());
        companyAccount.setClientId(dummyAccount.getClientId());
        companyAccount.setAccountOwnerType(AccountOwnerType.COMPANY);
        companyAccount.setCompanyId(1L);

        CompanyDto companyDto = new CompanyDto();
        companyDto.setId(1L);
        companyDto.setName("Test Company");
        companyDto.setMajorityOwner(dummyClient);

        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(companyAccount));
        when(cardRepository.save(any(Card.class))).thenReturn(dummyCard);
        when(userClient.getCompanyById(anyLong())).thenReturn(companyDto);

        // Act
        CardDto result = cardService.createCompanyCard(createDto, authHeader);

        // Assert
        assertNotNull(result);
        assertEquals(dummyCard.getCardNumber(), result.getCardNumber());
        verify(cardRepository).save(any(Card.class));
        verify(rabbitTemplate).convertAndSend(eq("card-creation"), any(EmailRequestDto.class));
    }

    @Test
    public void testCreateCompanyCard_ForAuthorizedPersonnel() {
        // Arrange
        CreateCompanyCardDto createDto = new CreateCompanyCardDto();
        createDto.setAccountNumber(dummyAccount.getAccountNumber());
        createDto.setCardLimit(BigDecimal.valueOf(3000));
        createDto.setForOwner(false);
        createDto.setAuthorizedPersonnelId(2L);

        // Create a proper CompanyAccount for this test
        CompanyAccount companyAccount = new CompanyAccount();
        companyAccount.setAccountNumber(dummyAccount.getAccountNumber());
        companyAccount.setClientId(dummyAccount.getClientId());
        companyAccount.setAccountOwnerType(AccountOwnerType.COMPANY);
        companyAccount.setCompanyId(1L);

        CompanyDto companyDto = new CompanyDto();
        companyDto.setId(1L);
        companyDto.setName("Test Company");
        companyDto.setMajorityOwner(dummyClient);

        ClientDto authorizedPerson = new ClientDto();
        authorizedPerson.setId(2L);
        authorizedPerson.setFirstName("Authorized");
        authorizedPerson.setLastName("Person");
        authorizedPerson.setEmail("authorized@example.com");

        AuthorizedPersonelDto authPersonDto = new AuthorizedPersonelDto();
        authPersonDto.setId(2L);
        authPersonDto.setFirstName("Authorized");
        authPersonDto.setLastName("Person");
        authPersonDto.setEmail("authorized@example.com");
        authPersonDto.setCompanyId(1L);

        List<AuthorizedPersonelDto> authorizedPersonnel = Arrays.asList(authPersonDto);

        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(companyAccount));
        when(cardRepository.save(any(Card.class))).thenReturn(dummyCard);
        when(userClient.getClientById(2L)).thenReturn(authorizedPerson);
        when(userClient.getCompanyById(anyLong())).thenReturn(companyDto);
        when(userClient.getAuthorizedPersonnelByCompany(anyLong())).thenReturn(authorizedPersonnel);

        // Act
        CardDto result = cardService.createCompanyCard(createDto, authHeader);

        // Assert
        assertNotNull(result);
        assertEquals(dummyCard.getCardNumber(), result.getCardNumber());
        verify(cardRepository).save(any(Card.class));
        verify(rabbitTemplate).convertAndSend(eq("card-creation"), any(EmailRequestDto.class));
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
        assertEquals(dummyCard.getCardNumber(), result.get(0).getCardNumber());
        assertEquals(dummyClient.getFirstName(), result.get(0).getOwner().getFirstName());
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
        when(userClient.getClientById(1L)).thenReturn(dummyClient);

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
        String nonExistentCardNumber = "9999888877776666";

        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(1L);
        when(cardRepository.findByCardNumber(nonExistentCardNumber)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> cardService.blockCardByUser(nonExistentCardNumber, authHeader));
    }

    @Test
    public void testBlockCardByUser_UnauthorizedUser() {
        // Arrange
        when(jwtAuthenticationFilter.getClaimsFromToken(anyString())).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(2L); // Different user ID
        when(cardRepository.findByCardNumber(dummyCard.getCardNumber())).thenReturn(Optional.of(dummyCard));

        // Act & Assert
        assertThrows(UnauthorizedException.class,
                () -> cardService.blockCardByUser(dummyCard.getCardNumber(), authHeader));
    }
}