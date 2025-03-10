package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.bank_service.controller.ExchangeRateController;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.exceptions.InvalidExchangeRateException;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;

public class ExchangeRateControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController exchangeRateController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Currency eur;
    private Currency rsd;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(exchangeRateController).build();

        eur = new Currency("EUR");
        rsd = new Currency("RSD");
    }

    /**
     * ✅ Testiranje dohvatanja postojećeg kursa
     */
    @Test
    @WithMockUser
    void testGetExchangeRate_Success() throws Exception {
        when(exchangeRateService.getExchangeRate(rsd.getCode(), eur.getCode())).thenReturn(new BigDecimal("117.5"));

        mockMvc.perform(get("/api/exchange-rates/RSD/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency").value("RSD"))
                .andExpect(jsonPath("$.toCurrency").value("EUR"))
                .andExpect(jsonPath("$.exchangeRate").value("117.5"));
    }

    /**
     * ❌ Testiranje kada kurs ne postoji
     */
    @Test
    @WithMockUser
    void testGetExchangeRate_NotFound() throws Exception {
        when(exchangeRateService.getExchangeRate(rsd.getCode(), eur.getCode())).thenThrow(new ExchangeRateNotFoundException("RSD", "EUR"));

        mockMvc.perform(get("/api/exchange-rates/RSD/EUR"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Error: Exchange rate not found for RSD to EUR"));
    }

    /**
     * ✅ Testiranje dodavanja novog kursa
     */
    @Test
    void testSaveOrUpdateExchangeRate_Success() throws Exception {
        ExchangeRateDto dto = new ExchangeRateDto();
        dto.setFromCurrency("RSD");
        dto.setToCurrency("EUR");
        dto.setExchangeRate(new BigDecimal("117.5"));

        ExchangeRate savedExchangeRate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("117.5"), null, null);

        when(exchangeRateService.saveOrUpdateExchangeRate("RSD", "EUR", new BigDecimal("0.0")))
                .thenThrow(new InvalidExchangeRateException("Exchange rate must be greater than zero."));

        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())  // ✅ Prikazuje telo odgovora
                .andExpect(status().isOk())
                .andExpect(content().string("Exchange rate saved successfully"));  // ✅ Očekujemo string
    }

    /**
     * ❌ Testiranje kada je unet nevalidan kurs
     */
    @Test
    void testSaveOrUpdateExchangeRate_InvalidRate() throws Exception {
        ExchangeRateDto dto = new ExchangeRateDto();
        dto.setFromCurrency("RSD");
        dto.setToCurrency("EUR");
        dto.setExchangeRate(new BigDecimal("0.0"));

        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.message.exchangeRate").value("Exchange rate must be greater than zero"));  // ✅ Proverava specifičnu validacionu grešku
    }
}
