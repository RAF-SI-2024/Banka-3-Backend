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
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.exceptions.CardLimitExceededException;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.security.JwtAuthenticationFilter;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.service.ExchangeRateService;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    @MockBean
    private ExchangeRateService exchangeRateService;
    @MockBean
    AccountMapper accountMapper;
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
    @WithMockUser(authorities = "employee")
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
    @WithMockUser(authorities = "employee")
    public void testBlockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.BLOCKED));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "employee")
    public void testUnblockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.ACTIVE));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "employee")
    public void testDeactivateCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.DEACTIVATED));

        mockMvc.perform(post("/api/account/123456789012345678/cards/1/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(authorities = "employee")
    public void testRequestCardForAccount_Success() throws Exception {
        // Kreiranje DTO-a
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        // Simulacija ponašanja servisa
        doNothing().when(cardService).requestCardForAccount(any(CreateCardDto.class));

        // Pozivamo API endpoint
        mockMvc.perform(post("/api/account/account123/cards/request")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(createCardDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("A confirmation email has been sent. Please verify to receive your card."));

        // ArgumentCaptor za hvatanje stvarnih argumenata koji su prosleđeni metodi
        ArgumentCaptor<CreateCardDto> captor = ArgumentCaptor.forClass(CreateCardDto.class);

        // Verifikacija da je metoda pozvana sa željenim argumentom
        verify(cardService, times(1)).requestCardForAccount(captor.capture());

        // Provera da li je argument koji je prosleđen metoda tačno isti kao onaj koji očekujemo
        CreateCardDto capturedDto = captor.getValue();
        assertEquals(createCardDto.getAccountNumber(), capturedDto.getAccountNumber());
        assertEquals(createCardDto.getType(), capturedDto.getType());
        assertEquals(createCardDto.getName(), capturedDto.getName());
        assertEquals(createCardDto.getCardLimit(), capturedDto.getCardLimit());
    }

    @Test
    @WithMockUser(authorities = "employee")
    public void testRequestCardForAccount_EntityNotFound() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("Visa", "John Doe", "account123", new BigDecimal("1000.00"));

        // Simulacija bacanja EntityNotFoundException u cardService
        doThrow(new EntityNotFoundException("Account with account number: account123 not found"))
                .when(cardService).requestCardForAccount(any(CreateCardDto.class));

        // Pozivamo API endpoint i očekujemo 404 Not Found status
        mockMvc.perform(post("/api/account/account123/cards/request")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(createCardDto)))
                .andExpect(status().isNotFound())  // Očekujemo 404 Not Found
                .andExpect(content().string("Account with account number: account123 not found"));  // Očekujemo telo sa porukom greške

        // Verifikacija da je metoda pozvana
        ArgumentCaptor<CreateCardDto> captor = ArgumentCaptor.forClass(CreateCardDto.class);
        verify(cardService, times(1)).requestCardForAccount(captor.capture());

        // Upoređivanje vrednosti objekta
        CreateCardDto capturedDto = captor.getValue();
        assertEquals("account123", capturedDto.getAccountNumber());
        assertEquals("Visa", capturedDto.getType());
        assertEquals("John Doe", capturedDto.getName());
        assertEquals(new BigDecimal("1000.00"), capturedDto.getCardLimit());
    }

    @Test
    @WithMockUser(authorities = "employee")
    public void testRequestCardForAccount_CardLimitExceeded() throws Exception {

        CreateCardDto createCardDto = new CreateCardDto("Visa", "John Doe", "account123", new BigDecimal("1000.00"));

        // Simulacija bacanja EntityNotFoundException u cardService
        doThrow(new CardLimitExceededException("account123"))
                .when(cardService).requestCardForAccount(any(CreateCardDto.class));

        // Pozivamo API endpoint i očekujemo 404 Not Found status
        mockMvc.perform(post("/api/account/account123/cards/request")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(createCardDto)))
                .andExpect(status().isBadRequest())  // Očekujemo 404 Not Found
                .andExpect(content().string("Card limit exceeded for the account with account number: account123"));  // Očekujemo telo sa porukom greške

        // Verifikacija da je metoda pozvana
        ArgumentCaptor<CreateCardDto> captor = ArgumentCaptor.forClass(CreateCardDto.class);
        verify(cardService, times(1)).requestCardForAccount(captor.capture());

        // Upoređivanje vrednosti objekta
        CreateCardDto capturedDto = captor.getValue();
        assertEquals("account123", capturedDto.getAccountNumber());
        assertEquals("Visa", capturedDto.getType());
        assertEquals("John Doe", capturedDto.getName());
        assertEquals(new BigDecimal("1000.00"), capturedDto.getCardLimit());
    }

    // Test for verifyAndReceiveCard
    @Test
    @WithMockUser(authorities = "employee")
    public void testVerifyAndReceiveCard_Success() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        // Full CardDtoNoOwner with all fields filled
        CardDtoNoOwner cardDto = new CardDtoNoOwner(
                1L,  // id
                "1234567890123456",  // cardNumber
                "123",  // cvv
                "Visa",  // type
                "John Doe",  // name
                LocalDate.of(2023, 1, 1),  // creationDate
                LocalDate.of(2027, 1, 1),  // expirationDate
                "account123",  // accountNumber
                CardStatus.ACTIVE,  // status
                new BigDecimal("1000.00")  // cardLimit
        );

        String token = "validToken";

        when(cardService.recieveCardForAccount(eq(token), eq(createCardDto))).thenReturn(cardDto);

        mockMvc.perform(post("/api/account/{accountNumber}/cards/recieve?token={token}", "account123", token)
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(createCardDto)))
                .andExpect(status().isOk()); // Očekujemo status 200 (OK)
    }


//    @Test
//    public void testVerifyAndReceiveCard_InvalidToken() throws Exception {
//        // Kreiranje DTO objekta
//        CreateCardDto createCardDto = new CreateCardDto("Visa", "John Doe", "account123", new BigDecimal("1000.00"));
//
//        // Simulacija bacanja InvalidTokenException u cardService
//        doThrow(new InvalidTokenException()).when(cardService).recieveCardForAccount(eq("invalidToken"), eq(createCardDto));
//
//        // ArgumentCaptor za CreateCardDto
//        ArgumentCaptor<CreateCardDto> captor = ArgumentCaptor.forClass(CreateCardDto.class);
//
//        // Pozivamo API endpoint i očekujemo 404 Not Found status
//        mockMvc.perform(post("/api/account/{accountNumber}/cards/recieve?token={token}", "account123", "invalidToken")
//                        .contentType("application/json")
//                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
//                .andExpect(status().isNotFound());
//
//        // Verifikacija da je metoda pozvana sa tačnim argumentima
//        verify(cardService, times(1)).recieveCardForAccount(eq("invalidToken"), captor.capture());
//
//        // Upoređivanje vrednosti objekta
//        CreateCardDto capturedDto = captor.getValue();
//        assertEquals("account123", capturedDto.getAccountNumber());
//        assertEquals("Visa", capturedDto.getType());
//        assertEquals("John Doe", capturedDto.getName());
//        assertEquals(new BigDecimal("1000.00"), capturedDto.getCardLimit());
//    }
//


    // Test for createCard
    @Test
    public void testCreateCard_Success() throws Exception {
        // Pripremi CreateCardDto objekat
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));
        String token = "token";

        // Priprema vraćenog objekta CardDtoNoOwner
        CardDtoNoOwner cardDto = new CardDtoNoOwner(
                1L,  // id
                "1234567890123456",  // cardNumber
                "123",  // cvv
                "Visa",  // type
                "John Doe",  // name
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
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isOk());

        // Verifikacija da je createCard pozvan sa tačnim argumentom
        verify(cardService, times(1)).createCard(captor.capture());

        // Upoređivanje vrednosti objekta
        CreateCardDto capturedDto = captor.getValue();
        assertEquals("account123", capturedDto.getAccountNumber());
        assertEquals("Visa", capturedDto.getType());
        assertEquals("John Doe", capturedDto.getName());
        assertEquals(new BigDecimal("1000.00"), capturedDto.getCardLimit());
    }


//    @Test
//    public void testCreateCard_EntityNotFound() throws Exception {
//        CreateCardDto createCardDto = new CreateCardDto( "Visa", "John Doe","account123", new BigDecimal("1000.00"));
//
//        doThrow(new EntityNotFoundException("Account not found")).when(cardService).createCard(createCardDto);
//
//        mockMvc.perform(post("/api/account/account123/cards/create")
//                        .contentType("application/json")
//                        .content(new ObjectMapper().writeValueAsString(createCardDto)))
//                .andExpect(status().isNotFound());
//
//        verify(cardService, times(1)).createCard(createCardDto);
//    }
}
