package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.CardController;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CardDtoNoOwner;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.enums.CardIssuer;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.enums.CardType;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.CardLimitExceededException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    @MockBean
    AccountMapper accountMapper;
    @MockBean
    private ExchangeRateService exchangeRateService;
    private MockMvc mockMvc;
    @Autowired
    private CardController cardController;
    @MockBean
    private CardService cardService;
    @MockBean
    private CardRepository cardRepository;
    @MockBean
    private UserClient userClient;
    @MockBean
    private RabbitTemplate rabbitTemplate;
    @MockBean
    private AccountRepository accountRepository;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(cardController).build();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testGetCardsByAccount() throws Exception {
        // Priprema test podataka
        String token = "valid token";
        CardDto cardDto = new CardDto();
        cardDto.setId(1L);
        cardDto.setCardNumber("1111222233334444");
        cardDto.setStatus(CardStatus.ACTIVE);

        List<CardDto> cards = List.of(cardDto);
        Mockito.when(cardService.getCardsByAccount(Mockito.anyString())).thenReturn(cards);

        // Testiranje API poziva
        mockMvc.perform(get("/api/account/123456789012345678/cards").header("Authorization", token)  // Ispravljena ruta
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber").value("1111222233334444"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testBlockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.BLOCKED));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testUnblockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.ACTIVE));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testDeactivateCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.DEACTIVATED));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    public void testRequestCardForAccount_Success() throws Exception {
        CreateCardDto requestDto = new CreateCardDto();
        requestDto.setType(CardType.CREDIT);
        requestDto.setIssuer(CardIssuer.VISA);
        requestDto.setName("Ime kartice");
        requestDto.setAccountNumber("account123");

        String authHeader = "Bearer validToken";

        doNothing().when(cardService).requestNewCard(eq(requestDto), eq(authHeader));

        mockMvc.perform(post("/api/account/account123/cards/request")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Card request sent for verification."));

        verify(cardService, times(1)).requestNewCard(any(), eq(authHeader));
    }


    @Test
    @WithMockUser(roles = "CLIENT")
    public void testRequestCardForAccount_EntityNotFound() throws Exception {
        CreateCardDto requestDto = new CreateCardDto();
        requestDto.setType(CardType.CREDIT);
        requestDto.setIssuer(CardIssuer.VISA);
        requestDto.setName("Ime kartice");
        requestDto.setAccountNumber("account123");

        String authHeader = "Bearer validToken";

        doThrow(new AccountNotFoundException())
                .when(cardService).requestNewCard(any(), eq(authHeader));


        mockMvc.perform(post("/api/account/account123/cards/request")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
        verify(cardService, times(1)).requestNewCard(any(), eq(authHeader));
    }


    @Test
    @WithMockUser(roles = "CLIENT")
    public void testRequestCardForAccount_CardLimitExceeded() throws Exception {
        CreateCardDto requestDto = new CreateCardDto();
        requestDto.setType(CardType.CREDIT);
        requestDto.setIssuer(CardIssuer.VISA);
        requestDto.setName("Ime kartice");
        requestDto.setAccountNumber("account123");

        String authHeader = "Bearer validToken";

        doThrow(new CardLimitExceededException("account123"))
                .when(cardService).requestNewCard(any(), eq(authHeader));

        mockMvc.perform(post("/api/account/account123/cards/request")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        verify(cardService, times(1)).requestNewCard(any(), eq(authHeader));
    }

    // Test for createCard
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    public void testCreateCard_Success() throws Exception {
        // Pripremi CreateCardDto objekat
        CreateCardDto createCardDto = new CreateCardDto(CardType.CREDIT, CardIssuer.VISA, "Ime kartice", "account123", new BigDecimal("1000.00"));
        String token = "token";

        // Priprema vraćenog objekta CardDtoNoOwner
        CardDtoNoOwner cardDto = new CardDtoNoOwner(
                1L,  // id
                "1234567890123456",  // cardNumber
                "123",  // cvv
                CardType.CREDIT,  // type
                CardIssuer.VISA,
                "Ime kartice",  // name
                LocalDate.of(2023, 1, 1),  // creationDate
                LocalDate.of(2027, 1, 1),  // expirationDate
                "account123",  // accountNumber
                CardStatus.ACTIVE,  // status
                new BigDecimal("1000.00")  // cardLimit
        );

        // Simulacija ponašanja cardService.createCard
        when(cardService.createCard(eq(createCardDto))).thenReturn(cardDto);

        // ArgumentCaptor za CreateCardDto
        ArgumentCaptor<CreateCardDto> captor = ArgumentCaptor.forClass(CreateCardDto.class);

        // Pozivanje API endpoint-a
        mockMvc.perform(post("/api/account/account123/cards/create")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isOk());

        // Verifikacija da je createCard pozvan sa tačnim argumentom
        verify(cardService, times(1)).createCard(captor.capture());

        // Upoređivanje vrednosti objekta
        CreateCardDto capturedDto = captor.getValue();
        assertEquals("account123", capturedDto.getAccountNumber());
        assertEquals(CardType.CREDIT, capturedDto.getType());
        assertEquals("Ime kartice", capturedDto.getName());
        assertEquals(new BigDecimal("1000.00"), capturedDto.getCardLimit());
    }
}
