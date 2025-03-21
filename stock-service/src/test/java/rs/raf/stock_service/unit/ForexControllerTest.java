package rs.raf.stock_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.stock_service.controller.ForexController;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.service.ForexService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ForexControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @Mock
    private ForexService forexService;
    @InjectMocks
    private ForexController forexController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(forexController).build();
    }

    @Test
    public void testGetForex_Success() throws Exception {
        ForexPairDto dto = new ForexPairDto();
        dto.setBaseCurrency("USD");
        dto.setQuoteCurrency("EUR");
        dto.setExchangeRate(new BigDecimal("0.85"));
        dto.setName("USD/EUR");
        dto.setTicker("USDEUR");
        dto.setLastRefresh(LocalDateTime.now());
        when(forexService.getForexPair("USD", "EUR")).thenReturn(dto);

        mockMvc.perform(get("/api/forex")
                        .param("base", "USD")
                        .param("quote", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.quoteCurrency").value("EUR"))
                .andExpect(jsonPath("$.exchangeRate").value(0.85));
    }

    @Test
    public void testGetAllForexPairs_Success() throws Exception {
        ForexPairDto dto1 = new ForexPairDto();
        dto1.setTicker("USDEUR");
        ForexPairDto dto2 = new ForexPairDto();
        dto2.setTicker("USDGBP");
        var page = new PageImpl<>(Arrays.asList(dto1, dto2), PageRequest.of(0, 10), 2);
        when(forexService.getForexPairsList(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/forex/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    public void testConvertCurrency_Success() throws Exception {
        when(forexService.getConversionRate("USD", "EUR")).thenReturn(new BigDecimal("0.85"));

        mockMvc.perform(get("/api/forex/convert") // 🔴 Was "/api/exchange/convert"
                        .param("base", "USD")
                        .param("target", "EUR")
                        .param("amount", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversion_rate").value(0.85))
                .andExpect(jsonPath("$.converted_amount").value(85.0));

    }
}
