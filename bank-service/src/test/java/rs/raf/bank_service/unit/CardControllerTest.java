package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.dto.CardDto;
import rs.raf.bank_service.service.CardService;
import rs.raf.bank_service.controller.CardController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    private CardDto testCardDto;
    private final Long CLIENT_ID = 1L;
    private final String CARD_NUMBER = "1234567890123456";

    @BeforeEach
    void setUp() {
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
    @WithMockUser(roles = "CLIENT")
    void getClientCards_ShouldReturnClientCards() throws Exception {
        // Arrange
        CardDto cardDto1 = testCardDto;

        CardDto cardDto2 = new CardDto();
        cardDto2.setId(2L);
        cardDto2.setCardNumber("6543210987654321");
        cardDto2.setStatus(CardStatus.ACTIVE);
        cardDto2.setAccountId(CLIENT_ID);

        List<CardDto> cards = Arrays.asList(cardDto1, cardDto2);

        when(cardService.getClientCards(CLIENT_ID)).thenReturn(cards);

        // Act & Assert
        mockMvc.perform(get("/api/cards/client/{clientId}", CLIENT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cardNumber", is(cardDto1.getCardNumber())))
                .andExpect(jsonPath("$[1].cardNumber", is(cardDto2.getCardNumber())));
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void blockCard_ShouldBlockCard() throws Exception {
        // Arrange
        CardDto blockedCardDto = new CardDto();
        blockedCardDto.setId(testCardDto.getId());
        blockedCardDto.setCardNumber(testCardDto.getCardNumber());
        blockedCardDto.setStatus(CardStatus.BLOCKED);
        blockedCardDto.setAccountId(CLIENT_ID);

        when(cardService.blockCard(CARD_NUMBER, CLIENT_ID)).thenReturn(blockedCardDto);

        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/block", CARD_NUMBER)
                .param("clientId", CLIENT_ID.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", is(CARD_NUMBER)))
                .andExpect(jsonPath("$.status", is(CardStatus.BLOCKED.toString())));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void unblockCard_ShouldUnblockCard() throws Exception {
        // Arrange
        CardDto unblockCardDto = new CardDto();
        unblockCardDto.setId(testCardDto.getId());
        unblockCardDto.setCardNumber(testCardDto.getCardNumber());
        unblockCardDto.setStatus(CardStatus.ACTIVE);
        unblockCardDto.setAccountId(CLIENT_ID);

        when(cardService.unblockCard(CARD_NUMBER)).thenReturn(unblockCardDto);

        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/unblock", CARD_NUMBER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", is(CARD_NUMBER)))
                .andExpect(jsonPath("$.status", is(CardStatus.ACTIVE.toString())));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deactivateCard_ShouldDeactivateCard() throws Exception {
        // Arrange
        CardDto deactivatedCardDto = new CardDto();
        deactivatedCardDto.setId(testCardDto.getId());
        deactivatedCardDto.setCardNumber(testCardDto.getCardNumber());
        deactivatedCardDto.setStatus(CardStatus.DEACTIVATED);
        deactivatedCardDto.setAccountId(CLIENT_ID);

        when(cardService.deactivateCard(CARD_NUMBER)).thenReturn(deactivatedCardDto);

        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/deactivate", CARD_NUMBER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber", is(CARD_NUMBER)))
                .andExpect(jsonPath("$.status", is(CardStatus.DEACTIVATED.toString())));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getClientCards_ShouldReturnForbidden_WhenUserDoesNotHaveClientRole() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/cards/client/{clientId}", CLIENT_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockCard_ShouldReturnForbidden_WhenUserDoesNotHaveClientRole() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/block", CARD_NUMBER)
                .param("clientId", CLIENT_ID.toString())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void unblockCard_ShouldReturnForbidden_WhenUserDoesNotHaveEmployeeRole() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/unblock", CARD_NUMBER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void deactivateCard_ShouldReturnForbidden_WhenUserDoesNotHaveEmployeeRole() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/cards/{cardNumber}/deactivate", CARD_NUMBER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}