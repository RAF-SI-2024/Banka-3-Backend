package rs.raf.bank_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.bank_service.controller.CardController;
import rs.raf.bank_service.domain.dto.CardDto;
import rs.raf.bank_service.domain.enums.CardStatus;
import rs.raf.bank_service.service.CardService;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @Autowired
    private ObjectMapper objectMapper;

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
        Mockito.doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)),
                Mockito.eq(CardStatus.BLOCKED));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/block")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "admin")
    public void testUnblockCard() throws Exception {
        Mockito.doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)),
                Mockito.eq(CardStatus.ACTIVE));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/unblock")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "admin")
    public void testDeactivateCard() throws Exception {
        Mockito.doNothing().when(cardService).changeCardStatus(String.valueOf(Mockito.eq(1L)),
                Mockito.eq(CardStatus.DEACTIVATED));

        mockMvc.perform(post("/api/accounts/123456789012345678/cards/1/deactivate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
