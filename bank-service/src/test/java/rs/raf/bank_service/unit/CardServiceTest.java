package rs.raf.bank_service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.*;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.domain.mapper.CardMapper;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.repository.CardRequestRepository;
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

public class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock private CardRepository cardRepository;
    @Mock private UserClient userClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private AccountRepository accountRepository;
    @Mock private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private CardRequestRepository cardRequestRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private ObjectMapper objectMapper;

    private final String authHeader = "Bearer test-token";
    private Account account;
    private ClientDto client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        account = new PersonalAccount();
        account.setAccountNumber("1234567890");
        account.setClientId(1L);
        client = new ClientDto(1L, "Test", "User", "test@example.com");
    }

    @Test
    void createCard_success() {
        CreateCardDto dto = new CreateCardDto(CardType.CREDIT, CardIssuer.VISA, "MyCard", account.getAccountNumber(), BigDecimal.valueOf(1000));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(0L);

        CardDtoNoOwner result = cardService.createCard(dto);
        assertNotNull(result);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_cardLimitExceeded() {
        CreateCardDto dto = new CreateCardDto(CardType.DEBIT, CardIssuer.MASTERCARD, "MyCard", account.getAccountNumber(), BigDecimal.valueOf(1000));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(3L);

        assertThrows(CardLimitExceededException.class, () -> cardService.createCard(dto));
    }

    @Test
    void createCard_invalidCardLimit() {
        CreateCardDto dto = new CreateCardDto(CardType.CREDIT, CardIssuer.VISA, "MyCard", account.getAccountNumber(), BigDecimal.ZERO);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(0L);

        assertThrows(InvalidCardLimitException.class, () -> cardService.createCard(dto));
    }

    @Test
    void changeCardStatus_success() {
        Card card = new Card();
        card.setAccount(account);
        when(cardRepository.findByCardNumber(anyString())).thenReturn(Optional.of(card));
        when(userClient.getClientById(anyLong())).thenReturn(client);

        cardService.changeCardStatus("1234", CardStatus.BLOCKED);
        verify(cardRepository).save(card);
        verify(rabbitTemplate).convertAndSend(eq("card-status-change"), any(EmailRequestDto.class));
    }

    @Test
    void changeCardStatus_cardNotFound() {
        when(cardRepository.findByCardNumber(anyString())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> cardService.changeCardStatus("xxx", CardStatus.BLOCKED));
    }

    @Test
    void blockCardByUser_success() {
        Card card = new Card();
        card.setAccount(account);
        card.setStatus(CardStatus.ACTIVE);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(account.getClientId());
        when(cardRepository.findByCardNumber(anyString())).thenReturn(Optional.of(card));
        when(userClient.getClientById(account.getClientId())).thenReturn(client);

        cardService.blockCardByUser("1111", authHeader);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
        verify(rabbitTemplate).convertAndSend(eq("card-status-change"), any(EmailRequestDto.class));
    }

    @Test
    void blockCardByUser_notAuthorized() {
        Card card = new Card();
        Account foreignAccount = new PersonalAccount();
        foreignAccount.setClientId(99L);
        card.setAccount(foreignAccount);

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(account.getClientId());
        when(cardRepository.findByCardNumber(anyString())).thenReturn(Optional.of(card));

        assertThrows(UnauthorizedException.class, () -> cardService.blockCardByUser("1234", authHeader));
    }

    @Test
    void getUserCards_returnsList() {
        Card card = new Card();
        card.setAccount(account);
        List<Account> accounts = List.of(account);
        account.setCards(List.of(card));

        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(account.getClientId());
        when(accountRepository.findByClientId(account.getClientId())).thenReturn(accounts);
        when(userClient.getClientById(account.getClientId())).thenReturn(client);

        List<CardDto> cards = cardService.getUserCards(authHeader);
        assertEquals(1, cards.size());
    }

    @Test
    void requestNewCard_success() throws JsonProcessingException {
        CreateCardDto dto = new CreateCardDto(CardType.CREDIT, CardIssuer.VISA, "MyCard", account.getAccountNumber(), BigDecimal.valueOf(1000));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(account.getClientId());
        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(0L);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        cardService.requestNewCard(dto, authHeader);

        verify(cardRequestRepository).save(any(CardRequest.class));
        verify(userClient).createVerificationRequest(any(CreateVerificationRequestDto.class));
    }

    @Test
    void requestNewCard_cardLimitExceeded() {
        CreateCardDto dto = new CreateCardDto(CardType.DEBIT, CardIssuer.VISA, "Test", account.getAccountNumber(), BigDecimal.valueOf(1000));
        when(jwtTokenUtil.getUserIdFromAuthHeader(authHeader)).thenReturn(account.getClientId());
        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(3L);

        assertThrows(CardLimitExceededException.class, () -> cardService.requestNewCard(dto, authHeader));
    }

    @Test
    void approveCardRequest_success() {
        CardRequest request = CardRequest.builder()
                .id(1L)
                .accountNumber(account.getAccountNumber())
                .clientId(account.getClientId())
                .cardType(CardType.DEBIT)
                .cardIssuer(CardIssuer.VISA)
                .cardLimit(BigDecimal.valueOf(500))
                .name("Approved")
                .status(RequestStatus.PENDING)
                .build();

        when(cardRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong())).thenReturn(Optional.of(account));
        when(accountMapper.toAccountTypeDto(account)).thenReturn(new AccountTypeDto(account.getAccountNumber(), AccountOwnerType.PERSONAL));
        when(cardRepository.countByAccount(account)).thenReturn(0L);

        cardService.approveCardRequest(1L);

        assertEquals(RequestStatus.APPROVED, request.getStatus());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void approveCardRequest_alreadyApproved() {
        CardRequest request = CardRequest.builder()
                .id(1L)
                .status(RequestStatus.APPROVED)
                .build();
        when(cardRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(EntityNotFoundException.class, () -> cardService.approveCardRequest(1L));
    }

    @Test
    void approveCardRequest_accountNotFound() {
        CardRequest request = CardRequest.builder()
                .id(1L)
                .status(RequestStatus.PENDING)
                .accountNumber("123")
                .clientId(1L)
                .build();

        when(cardRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(accountRepository.findByAccountNumberAndClientId(anyString(), anyLong())).thenReturn(Optional.empty());

        assertThrows(AccNotFoundException.class, () -> cardService.approveCardRequest(1L));
    }

}
