package rs.raf.stock_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.ExchangeRateApiClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.exceptions.*;
import rs.raf.stock_service.service.ForexService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ForexServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AlphavantageClient alphavantageClient;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private ForexService forexService;

    @BeforeEach
    void setUp() {
        // Any initialization needed.
    }

    @Test
    public void testGetConversionRate_Success() throws Exception {
        String conversionJson = "{ \"conversion_rate\": \"0.85\" }";
        when(exchangeRateApiClient.getConversionPair("", "USD", "EUR")).thenReturn(conversionJson);

        BigDecimal rate = forexService.getConversionRate("USD", "EUR");

        assertEquals(new BigDecimal("0.85"), rate);
    }

    @Test
    public void testGetConversionRate_Failure() {
        when(exchangeRateApiClient.getConversionPair("", "USD", "EUR"))
                .thenThrow(new RuntimeException("Error"));

        assertThrows(ExchangeRateConversionException.class, () -> forexService.getConversionRate("USD", "EUR"));
    }

    @Test
    public void testGetLatestRates_Success() throws Exception {
        String jsonRates = "{ \"conversion_rates\": { \"EUR\": \"0.85\", \"GBP\": \"0.75\" } }";
        when(exchangeRateApiClient.getLatestRates("", "USD")).thenReturn(jsonRates);

        Map<String, BigDecimal> expectedRates = new HashMap<>();
        expectedRates.put("EUR", new BigDecimal("0.85"));
        expectedRates.put("GBP", new BigDecimal("0.75"));

        Map<String, BigDecimal> actualRates = forexService.getLatestRates("USD");

        assertEquals(expectedRates, actualRates);
    }

    @Test
    public void testGetLatestRates_Failure() {
        when(exchangeRateApiClient.getLatestRates("", "USD"))
                .thenThrow(new RuntimeException("API Error"));

        assertThrows(LatestRatesNotFoundException.class, () -> forexService.getLatestRates("USD"));
    }

    @Test
    public void testGetForexPair_Success() throws Exception {
        String forexJson = "{ \"Realtime Currency Exchange Rate\": { " +
                "\"5. Exchange Rate\": \"1.10\", " +
                "\"9. Ask Price\": \"1.12\", " +
                "\"8. Bid Price\": \"1.08\", " +
                "\"6. Last Refreshed\": \"2024-03-20 12:00:00\" " +
                "} }";
        when(alphavantageClient.getCurrencyExchangeRate("USD", "EUR")).thenReturn(forexJson);

        ForexPairDto forexPair = forexService.getForexPair("USD", "EUR");

        assertNotNull(forexPair);
        assertEquals("USD", forexPair.getBaseCurrency());
        assertEquals("EUR", forexPair.getQuoteCurrency());
        assertEquals(new BigDecimal("1.10"), forexPair.getExchangeRate());
        assertEquals(new BigDecimal("1.12"), forexPair.getAsk());
        assertEquals(new BigDecimal("1.08"), forexPair.getPrice());
    }

    @Test
    public void testGetForexPair_Failure() {
        when(alphavantageClient.getCurrencyExchangeRate("USD", "EUR"))
                .thenThrow(new RuntimeException("Data not found"));

        assertThrows(ForexPairNotFoundException.class, () -> forexService.getForexPair("USD", "EUR"));
    }

    @Test
    public void testGetForexPairsList_Pagination_Success() throws Exception {
        String forexJson = "{ \"data\": [ " +
                "{\"symbol\": \"USD/EUR\"}," +
                "{\"symbol\": \"USD/GBP\"}" +
                "]}";
        when(twelveDataClient.getAllForexPairs("")).thenReturn(forexJson);

        PageRequest pageable = PageRequest.of(0, 2);
        Page<ForexPairDto> page = forexService.getForexPairsList(pageable);

        assertEquals(2, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }

    @Test
    public void testGetForexPairsList_Failure() {
        when(twelveDataClient.getAllForexPairs(""))
                .thenThrow(new RuntimeException("Data retrieval error"));

        PageRequest pageable = PageRequest.of(0, 2);
        assertThrows(ForexPairsNotFoundException.class, () -> forexService.getForexPairsList(pageable));
    }
}
