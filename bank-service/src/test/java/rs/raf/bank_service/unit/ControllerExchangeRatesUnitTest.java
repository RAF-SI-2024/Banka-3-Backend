package rs.raf.bank_service.unit;

import static org.mockito.Mockito.*;
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
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.CurrencyDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.util.Collections;

public class ControllerExchangeRatesUnitTest {

    private MockMvc mockMvc;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController exchangeRateController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CurrencyDto eur;
    private CurrencyDto rsd;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(exchangeRateController).build();

        eur = new CurrencyDto("EUR", "Euro", "€");
        rsd = new CurrencyDto("RSD", "Dinar", "RSD");
    }

    @Test
    @WithMockUser
    void testGetExchangeRate_Success() throws Exception {
        ExchangeRateDto exchangeRateDto = new ExchangeRateDto(rsd, eur, new BigDecimal("118.0"), new BigDecimal("119.0"));

        when(exchangeRateService.getExchangeRate("RSD", "EUR")).thenReturn(exchangeRateDto);

        mockMvc.perform(get("/api/exchange-rates/RSD/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCurrency.code").value("RSD"))
                .andExpect(jsonPath("$.toCurrency.code").value("EUR"))
                .andExpect(jsonPath("$.exchangeRate").value("118.0"))
                .andExpect(jsonPath("$.sellRate").value("119.0"));
    }

    @Test
    @WithMockUser
    void testGetExchangeRate_NotFound() throws Exception {
        when(exchangeRateService.getExchangeRate("RSD", "EUR"))
                .thenThrow(new ExchangeRateNotFoundException("RSD", "EUR"));

        mockMvc.perform(get("/api/exchange-rates/RSD/EUR"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Error: Exchange rate from RSD to EUR not found."));
    }

    @Test
    @WithMockUser
    void testConvert_Success() throws Exception {
        ConvertDto convertDto = new ConvertDto("RSD", "EUR", new BigDecimal("1000"));
        BigDecimal convertedAmount = new BigDecimal("8.514");

        when(exchangeRateService.convert(any(ConvertDto.class))).thenReturn(convertedAmount);

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("8.514"));
    }

    @Test
    @WithMockUser
    void testConvert_ExchangeRateNotFound() throws Exception {
        ConvertDto convertDto = new ConvertDto("RSD", "GBP", new BigDecimal("1000"));

        when(exchangeRateService.convert(any(ConvertDto.class)))
                .thenThrow(new ExchangeRateNotFoundException("RSD", "GBP"));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Exchange rate from RSD to GBP not found."));
    }

    @Test
    @WithMockUser
    void testConvert_CurrencyNotFound() throws Exception {
        ConvertDto convertDto = new ConvertDto("RSD", "XYZ", new BigDecimal("1000"));

        when(exchangeRateService.convert(any(ConvertDto.class)))
                .thenThrow(new CurrencyNotFoundException("XYZ"));

        mockMvc.perform(post("/api/exchange-rates/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convertDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Currency not found: XYZ"));
    }


    @Test
    @WithMockUser
    void testGetExchangeRates_Success() throws Exception {
        ExchangeRateDto dto = new ExchangeRateDto(rsd, eur, new BigDecimal("117.5"), new BigDecimal("118.7"));
        when(exchangeRateService.getExchangeRates()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromCurrency.code").value("RSD"))
                .andExpect(jsonPath("$[0].toCurrency.code").value("EUR"))
                .andExpect(jsonPath("$[0].exchangeRate").value("117.5"))
                .andExpect(jsonPath("$[0].sellRate").value("118.7"));
    }

    @Test
    @WithMockUser
    void testGetExchangeRates_InternalError() throws Exception {
        when(exchangeRateService.getExchangeRates()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error retrieving exchange rates."));
    }
}
