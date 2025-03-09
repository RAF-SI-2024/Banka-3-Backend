package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.controller.CardController;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.dto.CardDtoNoOwner;
import rs.raf.bank_service.domain.dto.CreateCardDto;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.exceptions.CardLimitExceededException;
import rs.raf.bank_service.exceptions.InvalidTokenException;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserClient userClient;

    @MockBean
    private ChangeLimitRequestRepository changeLimitRequestRepository;

    @Test
    @WithMockUser(authorities = "admin")
    public void testGetCardsByAccount() throws Exception {
        CardDto cardDto = new CardDto();
        cardDto.setId(1L);
        cardDto.setCardNumber("1111222233334444");
        cardDto.setStatus(CardStatus.ACTIVE);

        List<CardDto> cards = Arrays.asList(cardDto);
        Mockito.when(cardService.getCardsByAccount(Mockito.anyString())).thenReturn(cards);

        mockMvc.perform(get("/api/accounts/123456789012345678/cards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber").value("1111222233334444"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "admin")
    public void testBlockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.BLOCKED));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/block")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "admin")
    public void testUnblockCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/unblock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "admin")
    public void testDeactivateCard() throws Exception {
        doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)), Mockito.eq(CardStatus.DEACTIVATED));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }




    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRequestCardForAccount_Success() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        doNothing().when(cardService).requestCardForAccount(createCardDto);

        mockMvc.perform(post("/request")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isOk())
                .andExpect(content().string("A confirmation email has been sent. Please verify to receive your card."));

        verify(cardService, times(1)).requestCardForAccount(createCardDto);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRequestCardForAccount_EntityNotFound() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        doThrow(new EntityNotFoundException("Account not found")).when(cardService).requestCardForAccount(createCardDto);

        mockMvc.perform(post("/request")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(cardService, times(1)).requestCardForAccount(createCardDto);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testRequestCardForAccount_CardLimitExceeded() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        doThrow(new CardLimitExceededException("Card limit exceeded")).when(cardService).requestCardForAccount(createCardDto);

        mockMvc.perform(post("/request")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Card limit exceeded"));

        verify(cardService, times(1)).requestCardForAccount(createCardDto);
    }

    // Test for verifyAndReceiveCard
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
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
                CardStatus.ACTIVE,  // status (Example, adjust to your actual enum)
                new BigDecimal("1000.00")  // cardLimit
        );

        when(cardService.recieveCardForAccount("validToken", createCardDto)).thenReturn(cardDto);

        mockMvc.perform(post("/recieve?token=validToken")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"cardNumber\":\"1234567890123456\",\"cvv\":\"123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"creationDate\":\"2023-01-01\",\"expirationDate\":\"2027-01-01\",\"accountNumber\":\"account123\",\"status\":\"ACTIVE\",\"cardLimit\":1000.00}"));

        verify(cardService, times(1)).recieveCardForAccount("validToken", createCardDto);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testVerifyAndReceiveCard_InvalidToken() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        doThrow(new InvalidTokenException()).when(cardService).recieveCardForAccount("invalidToken", createCardDto);

        mockMvc.perform(post("/recieve?token=invalidToken")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).recieveCardForAccount("invalidToken", createCardDto);
    }

    // Test for createCard
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateCard_Success() throws Exception {
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
                CardStatus.ACTIVE,  // status (Example, adjust to your actual enum)
                new BigDecimal("1000.00")  // cardLimit
        );

        when(cardService.createCard(createCardDto)).thenReturn(cardDto);

        mockMvc.perform(post("/create")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1,\"cardNumber\":\"1234567890123456\",\"cvv\":\"123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"creationDate\":\"2023-01-01\",\"expirationDate\":\"2027-01-01\",\"accountNumber\":\"account123\",\"status\":\"ACTIVE\",\"cardLimit\":1000.00}"));

        verify(cardService, times(1)).createCard(createCardDto);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateCard_EntityNotFound() throws Exception {
        CreateCardDto createCardDto = new CreateCardDto("account123", "Visa", "John Doe", new BigDecimal("1000.00"));

        doThrow(new EntityNotFoundException("Account not found")).when(cardService).createCard(createCardDto);

        mockMvc.perform(post("/create")
                        .contentType("application/json")
                        .content("{\"accountNumber\":\"account123\",\"type\":\"Visa\",\"name\":\"John Doe\",\"cardLimit\":1000.00}"))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).createCard(createCardDto);
    }
}
