package rs.raf.stock_service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.StockDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.service.StocksService;

@ExtendWith(MockitoExtension.class)
public class StocksServiceTest {

    @Mock
    private AlphavantageClient alphavantageClient;

    @Mock
    private TwelveDataClient twelveDataClient;

    @InjectMocks
    private StocksService stockService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Po potrebi podesite inicijalne vrednosti.
    }

    @Test
    public void testGetStockData_Success() throws Exception {
        // Pripremite sample JSON odgovore
        String globalQuoteJson = "{ \"Global Quote\": { " +
                "\"05. price\": \"150.00\", " +
                "\"09. change\": \"2.50\", " +
                "\"06. volume\": \"1000000\" " +
                "} }";

        String overviewJson = "{ " +
                "\"SharesOutstanding\": \"1000000\", " +
                "\"DividendYield\": \"0.02\", " +
                "\"Name\": \"Test Company\", " +
                "\"exchange\": \"NYSE\" " +
                "}";

        when(alphavantageClient.getGlobalQuote("TEST")).thenReturn(globalQuoteJson);
        when(alphavantageClient.getCompanyOverview("TEST")).thenReturn(overviewJson);

        StockDto dto = stockService.getStockData("TEST");
        assertNotNull(dto);
        assertEquals("TEST", dto.getSymbol());
        assertEquals("Test Company", dto.getName());
        //assertEquals("NYSE", dto.getExchange());
        assertEquals(new BigDecimal("150.00"), dto.getPrice());
        assertEquals(new BigDecimal("2.50"), dto.getChange());
        // Market cap = 1,000,000 * 150 = 150000000
        assertEquals(new BigDecimal("150000000").stripTrailingZeros(), dto.getMarketCap().stripTrailingZeros());
    }

    @Test
    public void testGetStockData_NotFound() throws Exception {
        when(alphavantageClient.getGlobalQuote("INVALID")).thenThrow(new RuntimeException("Not Found"));
        assertThrows(StockNotFoundException.class, () -> stockService.getStockData("INVALID"));
    }

    @Test
    public void testGetStocksList_Pagination() throws Exception {
        String stocksJson = "{ \"data\": [ " +
                "{\"symbol\": \"AAPL\", \"name\": \"Apple Inc.\", \"mic_code\": \"XNAS\"}," +
                "{\"symbol\": \"MSFT\", \"name\": \"Microsoft Corp.\", \"mic_code\": \"XNAS\"}" +
                "]}";
        when(twelveDataClient.getAllStocks("")).thenReturn(stocksJson);

        // Explicitly create a pageable object
        Pageable pageable = PageRequest.of(0, 2);

        Page<StockDto> page = stockService.getStocksList(pageable);
        assertEquals(2, page.getTotalElements()); // Expecting 2 stocks
    }

}
