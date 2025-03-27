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
import org.springframework.data.domain.Pageable;
import rs.raf.stock_service.client.AlphavantageClient;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.domain.dto.StockSearchDto;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.exceptions.StockNotFoundException;
import rs.raf.stock_service.exceptions.SymbolSearchException;
import rs.raf.stock_service.service.ExchangeService;
import rs.raf.stock_service.service.StocksService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StocksServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private AlphavantageClient alphavantageClient;
    @Mock
    private TwelveDataClient twelveDataClient;
    @InjectMocks
    private StocksService stockService;
    @Mock
    private ExchangeService exchangeService;

    @BeforeEach
    void setUp() {
        // Po potrebi podesite inicijalne vrednosti.
    }

    @Test
    public void testGetStockData_Success() throws Exception {
        Exchange ex1 = new Exchange();
        ex1.setMic("NYSE");

        Exchange ex2 = new Exchange();
        ex2.setMic("XNAS");

        when(exchangeService.getAvailableExchanges()).thenReturn(List.of(ex1, ex2));

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
        assertEquals("TEST", dto.getTicker());
        assertEquals("Test Company", dto.getName());
        assertEquals(new BigDecimal("150.00"), dto.getPrice());
        assertEquals(new BigDecimal("2.50"), dto.getChange());
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

    @Test
    public void testSearchByTicker_Success() throws Exception {
        // Sample JSON response from AlphavantageClient
        String searchJson = "{ \"bestMatches\": [ " +
                "{ \"1. symbol\": \"AAPL\", \"2. name\": \"Apple Inc.\", \"4. region\": \"United States\", \"9. matchScore\": \"0.95\" }," +
                "{ \"1. symbol\": \"MSFT\", \"2. name\": \"Microsoft Corporation\", \"4. region\": \"United States\", \"9. matchScore\": \"0.90\" }" +
                "] }";

        when(alphavantageClient.searchByTicker("AAPL")).thenReturn(searchJson);

        List<StockSearchDto> results = stockService.searchByTicker("AAPL");

        assertNotNull(results);
        assertEquals(2, results.size());

        assertEquals("AAPL", results.get(0).getTicker());
        assertEquals("Apple Inc.", results.get(0).getName());
        assertEquals("United States", results.get(0).getRegion());
        assertEquals("0.95", results.get(0).getMatchScore());

        assertEquals("MSFT", results.get(1).getTicker());
        assertEquals("Microsoft Corporation", results.get(1).getName());
        assertEquals("United States", results.get(1).getRegion());
        assertEquals("0.90", results.get(1).getMatchScore());
    }

    @Test
    public void testSearchByTicker_EmptyResponse() throws Exception {
        // Empty bestMatches array
        String searchJson = "{ \"bestMatches\": [] }";

        when(alphavantageClient.searchByTicker("XYZ")).thenReturn(searchJson);

        List<StockSearchDto> results = stockService.searchByTicker("XYZ");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchByTicker_Error() {
        when(alphavantageClient.searchByTicker("INVALID"))
                .thenThrow(new RuntimeException("API Error"));

        assertThrows(SymbolSearchException.class, () -> stockService.searchByTicker("INVALID"));
    }


}
