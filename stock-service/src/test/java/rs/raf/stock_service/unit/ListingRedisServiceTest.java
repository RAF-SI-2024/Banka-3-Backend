package rs.raf.stock_service.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.entity.Listing;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.service.ListingRedisService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListingRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private ListingPriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private ListingRedisService listingRedisService;

    private Listing listing;
    private ListingDto listingDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        listing = new Stock();
        listing.setId(1L);
        listing.setTicker("AAPL");

        listingDto = new ListingDto();
        listingDto.setTicker("AAPL");
        listingDto.setPrice(BigDecimal.valueOf(150.00));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testSaveByTicker() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(listingDto)).thenReturn("{\"ticker\":\"AAPL\"}");

        listingRedisService.saveByTicker(listingDto);

        verify(valueOperations).set(eq("listing:AAPL"), anyString(), eq(Duration.ofHours(6)));      }

    @Test
    void testGetByTicker() throws JsonProcessingException {
        String json = "{\"ticker\":\"AAPL\"}";
        when(valueOperations.get("listing:AAPL")).thenReturn(json);
        when(objectMapper.readValue(json, ListingDto.class)).thenReturn(listingDto);

        ListingDto result = listingRedisService.getByTicker("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
    }

    @Test
    void testSaveAll() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"ticker\":\"AAPL\"}");

        listingRedisService.saveAll(List.of(listingDto));

        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofHours(6)));
    }

    @Test
    void testGetAllListings() throws JsonProcessingException {
        when(redisTemplate.keys("listing:*")).thenReturn(Set.of("listing:AAPL"));
        when(valueOperations.get("listing:AAPL")).thenReturn("{\"ticker\":\"AAPL\"}");
        when(objectMapper.readValue(anyString(), eq(ListingDto.class))).thenReturn(listingDto);

        List<ListingDto> results = listingRedisService.getAllListings();

        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getTicker());
    }

    @Test
    void testClear() {
        Set<String> keys = Set.of("listing:AAPL", "listing:GOOGL");
        when(redisTemplate.keys("*")).thenReturn(keys);

        listingRedisService.clear();

        for (String key : keys) {
            verify(redisTemplate).delete(key);
        }
    }
}
