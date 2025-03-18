package rs.raf.stock_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.stock_service.controller.ExchangeController;
import rs.raf.stock_service.domain.dto.ConversionResponseDto;
import rs.raf.stock_service.service.ForexService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(ExchangeController.class)
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForexService forexService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;  // Dodajemo mock za JwtTokenUtil da ne bismo dizali ceo Security kontekst

    @Test
    void testConvertCurrency_withAmount() throws Exception {
        String base = "USD";
        String target = "EUR";
        BigDecimal amount = new BigDecimal("100");
        BigDecimal conversionRate = new BigDecimal("0.85");
        BigDecimal convertedAmount = amount.multiply(conversionRate);

        ConversionResponseDto responseDto = new ConversionResponseDto();
        responseDto.setConversionRate(conversionRate);
        responseDto.setConvertedAmount(convertedAmount);

        when(forexService.getConversionRate(base, target)).thenReturn(conversionRate);

        mockMvc.perform(get("/api/exchange/convert")
                        .param("base", base)
                        .param("target", target)
                        .param("amount", amount.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionRate").value(conversionRate.doubleValue()))
                .andExpect(jsonPath("$.convertedAmount").value(convertedAmount.doubleValue())); // Očekujemo double umesto string formata

    }

    @Test
    void testConvertCurrency_withoutAmount() throws Exception {
        String base = "USD";
        String target = "EUR";
        BigDecimal conversionRate = new BigDecimal("0.85");

        when(forexService.getConversionRate(base, target)).thenReturn(conversionRate);

        mockMvc.perform(get("/api/exchange/convert")
                        .param("base", base)
                        .param("target", target)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversionRate").value(conversionRate))
                .andExpect(jsonPath("$.convertedAmount").doesNotExist());
    }

    @Test
    void testGetLatestRates() throws Exception {
        String base = "USD";
        Map<String, BigDecimal> rates = Collections.singletonMap("EUR", new BigDecimal("0.85"));

        when(forexService.getLatestRates(base)).thenReturn(rates);

        mockMvc.perform(get("/api/exchange/latest")
                        .param("base", base)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EUR").value("0.85"));
    }
}
