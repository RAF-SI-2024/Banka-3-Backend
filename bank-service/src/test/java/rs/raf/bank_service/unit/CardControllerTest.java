package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.controller.CardController;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.enums.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.service.ExchangeRateService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    private CreateCardDto createCardDto;

    @BeforeEach
    void setup() {
        createCardDto = new CreateCardDto(
                CardType.CREDIT,
                CardIssuer.VISA,
                "Test Card",
                "1234567890123456",
                BigDecimal.valueOf(1000)
        );
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetCardsByAccount_success() throws Exception {
        CardDto dto = new CardDto();
        dto.setCardNumber("1111");
        dto.setStatus(CardStatus.ACTIVE);
        when(cardService.getCardsByAccount("123")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/account/123/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber").value("1111"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetCardsByAccount_notFound() throws Exception {
        when(cardService.getCardsByAccount("123")).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/api/account/123/cards"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testBlockCard_success() throws Exception {
        doNothing().when(cardService).changeCardStatus("1111", CardStatus.BLOCKED);

        mockMvc.perform(post("/api/account/123/cards/1111/block"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testBlockCard_notFound() throws Exception {
        Mockito.doThrow(new CardNotFoundException("not found")).when(cardService).changeCardStatus("1111", CardStatus.BLOCKED);

        mockMvc.perform(post("/api/account/123/cards/1111/block"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRequestNewCard_success() throws Exception {
        doNothing().when(cardService).requestNewCard(any(), any());

        mockMvc.perform(post("/api/account/123/cards/request")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRequestNewCard_accountNotFound() throws Exception {
        Mockito.doThrow(new AccountNotFoundException()).when(cardService).requestNewCard(any(), any());

        mockMvc.perform(post("/api/account/123/cards/request")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testRequestNewCard_limitExceeded() throws Exception {
        Mockito.doThrow(new CardLimitExceededException("123"))
                .when(cardService).requestNewCard(any(), any());

        mockMvc.perform(post("/api/account/123/cards/request")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateCard_success() throws Exception {
        CardDtoNoOwner dto = new CardDtoNoOwner(1L, "1234", "999", CardType.CREDIT, CardIssuer.VISA, "Card", LocalDate.now(), LocalDate.now().plusYears(4), "acc", CardStatus.ACTIVE, BigDecimal.valueOf(1000));
        when(cardService.createCard(any())).thenReturn(dto);

        mockMvc.perform(post("/api/account/123/cards/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("1234"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateCard_badRequest() throws Exception {
        when(cardService.createCard(any())).thenThrow(new InvalidCardLimitException());

        mockMvc.perform(post("/api/account/123/cards/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testCreateCard_notFound() throws Exception {
        when(cardService.createCard(any())).thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(post("/api/account/123/cards/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testBlockCardByUser_success() throws Exception {
        doNothing().when(cardService).blockCardByUser("1111", "Bearer token");

        mockMvc.perform(post("/api/account/123/cards/1111/block-by-user")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testBlockCardByUser_notFound() throws Exception {
        Mockito.doThrow(new EntityNotFoundException("not found")).when(cardService).blockCardByUser("1111", "Bearer token");

        mockMvc.perform(post("/api/account/123/cards/1111/block-by-user")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUnblockCard_success() throws Exception {
        doNothing().when(cardService).changeCardStatus("1111", CardStatus.ACTIVE);

        mockMvc.perform(post("/api/account/123/cards/1111/unblock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testUnblockCard_notFound() throws Exception {
        Mockito.doThrow(new CardNotFoundException("not found")).when(cardService).changeCardStatus("1111", CardStatus.ACTIVE);

        mockMvc.perform(post("/api/account/123/cards/1111/unblock"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDeactivateCard_success() throws Exception {
        doNothing().when(cardService).changeCardStatus("1111", CardStatus.DEACTIVATED);

        mockMvc.perform(post("/api/account/123/cards/1111/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDeactivateCard_notFound() throws Exception {
        Mockito.doThrow(new EntityNotFoundException()).when(cardService).changeCardStatus("1111", CardStatus.DEACTIVATED);

        mockMvc.perform(post("/api/account/123/cards/1111/deactivate"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetUserCards_success() throws Exception {
        CardDto dto = new CardDto();
        dto.setCardNumber("1234");
        when(cardService.getUserCards("Bearer token")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/account/123/cards/my-cards")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber").value("1234"));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void testGetUserCardsForAccount_success() throws Exception {
        CardDto dto = new CardDto();
        dto.setCardNumber("5678");
        when(cardService.getUserCardsForAccount("123", "Bearer token")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/account/123/cards/my-account-cards")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardNumber").value("5678"));
    }
}
