package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.ListingFilterDto;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.service.ListingService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingDailyPriceInfoRepository dailyPriceInfoRepository;

    @Mock
    private ListingMapper listingMapper;

    @InjectMocks
    private ListingService listingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getListings_ShouldReturnListOfDtos() {
        // Mock podaci za Stock
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");
        stock.setPrice(new BigDecimal("150.50"));

        ListingDailyPriceInfo dailyInfo = new ListingDailyPriceInfo();
        dailyInfo.setChange(new BigDecimal("2.50"));
        dailyInfo.setVolume(2000000L);

        ListingDto expectedDto = new ListingDto(1L, "AAPL", new BigDecimal("150.50"), new BigDecimal("2.50"), 2000000L, new BigDecimal("165.55"));

        // Mock ponašanje repozitorijuma
        when(listingRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(stock));

        when(dailyPriceInfoRepository.findTopByListingOrderByDateDesc(stock)).thenReturn(dailyInfo);
        when(listingMapper.toDto(stock, dailyInfo)).thenReturn(expectedDto);

        // Poziv metode
        List<ListingDto> result = listingService.getListings(new ListingFilterDto(), "CLIENT");

        // Provera rezultata
        assertEquals(1, result.size());
        assertEquals(expectedDto, result.get(0));

        // Verifikacija poziva
        verify(listingRepository, times(1)).findAll(any(Specification.class));
        verify(dailyPriceInfoRepository, times(1)).findTopByListingOrderByDateDesc(stock);
        verify(listingMapper, times(1)).toDto(stock, dailyInfo);
    }
}
