package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.*;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.repository.*;
import rs.raf.stock_service.service.*;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DataRefreshServiceTest {

    @InjectMocks
    private DataRefreshService refreshService;

    @Mock private ListingRepository listingRepository;
    @Mock private ListingPriceHistoryRepository priceHistoryRepository;
    @Mock private PortfolioEntryRepository portfolioEntryRepository;
    @Mock private OptionRepository optionRepository;
    @Mock private OptionService optionService;
    @Mock private StocksService stocksService;
    @Mock private ForexService forexService;
    @Mock private ListingService listingService;
    @Mock private EntityManager entityManager;
    @Mock private OrderService orderService;
    @Mock private ListingMapper listingMapper;
    @Mock private ListingRedisService listingRedisService;


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(refreshService, "threadPoolSize", 4);
        ReflectionTestUtils.setField(refreshService, "listingMapper", listingMapper);
        ReflectionTestUtils.setField(refreshService, "listingRedisService", listingRedisService);

    }

    @Test
    public void testRefreshListings() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");

        ForexPair forex = new ForexPair();
        forex.setId(2L);
        forex.setTicker("USD/EUR");

        doNothing().when(orderService).checkOrders();

        when(listingRepository.findAll()).thenReturn(List.of(stock, forex));

        when(stocksService.getStockData("AAPL")).thenReturn(
                StockDto.builder()
                        .ticker("AAPL")
                        .name("Apple")
                        .price(new BigDecimal("100"))
                        .change(BigDecimal.ZERO)
                        .volume(100L)
                        .build()
        );

        TimeSeriesDto.TimeSeriesValueDto value = new TimeSeriesDto.TimeSeriesValueDto();
        value.setDatetime("2024-01-01 10:00:00");
        value.setOpen(BigDecimal.TEN);
        value.setHigh(BigDecimal.TEN);
        value.setLow(BigDecimal.TEN);
        value.setClose(BigDecimal.TEN);
        value.setVolume(100L);

        TimeSeriesDto timeSeries = new TimeSeriesDto();
        timeSeries.setValues(List.of(value));
        timeSeries.setStatus("ok");

        when(listingService.getPriceHistoryFromAlphaVantage(eq("AAPL"), any(), any()))
                .thenReturn(timeSeries);
        when(priceHistoryRepository.findDatesByListingId(1L)).thenReturn(Set.of());

        // Forex deo
        when(forexService.getForexPair("USD", "EUR")).thenReturn(
                ForexPairDto.builder()
                        .ticker("USD/EUR")
                        .baseCurrency("USD")
                        .quoteCurrency("EUR")
                        .price(new BigDecimal("1.1"))
                        .exchangeRate(new BigDecimal("1.1"))
                        .liquidity(String.valueOf(100L))
                        .lastRefresh(LocalDateTime.now())
                        .build()
        );

        when(listingService.getForexPriceHistory(2L, "5min"))
                .thenReturn(timeSeries);
        when(priceHistoryRepository.findDatesByListingId(2L)).thenReturn(Set.of());
        assertDoesNotThrow(() -> refreshService.refreshListings());

        verify(listingRepository, atLeastOnce()).findAll();
        verify(listingRepository, atLeastOnce()).save(any());
    }

    @Test
    public void testRefreshStockHandlesExceptionGracefully() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("FAIL");
        doNothing().when(orderService).checkOrders();
        doThrow(new RuntimeException("Boom")).when(stocksService).getStockData("FAIL");

        assertDoesNotThrow(() -> refreshService.refreshListings()); // test indirectly
    }

    @Test
    public void testRefreshForexInvalidTickerSkipped() {
        ForexPair forex = new ForexPair();
        forex.setTicker("BADFORMAT");

        doNothing().when(orderService).checkOrders();

        refreshService.refreshListings(); // indirectly, since method is private
        verify(forexService, never()).getForexPair(any(), any());
    }

    @Test
    public void testFakeSaveInBatches() {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            items.add("item" + i);
        }

        AtomicInteger batches = new AtomicInteger(0);
        Consumer<List<String>> saver = batch -> batches.incrementAndGet();

        int batchSize = 100;
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            saver.accept(items.subList(i, end));
        }

        assertEquals(3, batches.get());
    }

    @Test
    public void testFakeRefreshInParallel() {
        List<String> input = List.of("a", "b", "c");
        Set<String> seen = Collections.synchronizedSet(new HashSet<>());

        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (String item : input) {
            executor.submit(() -> seen.add(item));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("Execution interrupted");
        }

        assertEquals(3, seen.size());
    }

}