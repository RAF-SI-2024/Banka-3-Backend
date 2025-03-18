package rs.raf.stock_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.ExchangeRateApiClient;
import rs.raf.stock_service.client.TwelveDataClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import rs.raf.stock_service.exceptions.ExchangeRateConversionException;
import rs.raf.stock_service.service.ForexService;

@ExtendWith(MockitoExtension.class)
public class ForexServiceTest {

    @Mock
    private AlphavantageClient alphavantageClient;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private ForexService forexService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inicijalizujte po potrebi.
    }

    @Test
    public void testGetConversionRate_Success() throws Exception {
        String conversionJson = "{ \"conversion_rate\": \"0.85\" }";
        when(exchangeRateApiClient.getConversionPair("", "USD", "EUR")).thenReturn(conversionJson);
        BigDecimal rate = forexService.getConversionRate("USD", "EUR");
        assertEquals(new BigDecimal("0.85"), rate);
    }

    @Test
    public void testGetConversionRate_Failure() throws Exception {
        when(exchangeRateApiClient.getConversionPair("", "USD", "EUR")).thenThrow(new RuntimeException("Error"));
        assertThrows(ExchangeRateConversionException.class, () -> forexService.getConversionRate("USD", "EUR"));
    }

    @Test
    public void testGetForexPairsList_Pagination() throws Exception {
        String forexJson = "{ \"data\": [ " +
                "{\"symbol\": \"USD/EUR\"}," +
                "{\"symbol\": \"USD/GBP\"}" +
                "]}";
        when(twelveDataClient.getAllForexPairs("")).thenReturn(forexJson);

        PageRequest pageable = PageRequest.of(0, 2); // Stvarni Pageable objekat
        Page<?> page = forexService.getForexPairsList(pageable);

        assertEquals(2, page.getContent().size()); // Ispravljeno očekivanje
        assertEquals(2, page.getTotalElements());
    }

}
