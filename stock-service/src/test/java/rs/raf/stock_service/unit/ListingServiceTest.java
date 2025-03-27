package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.domain.entity.ListingDailyPriceInfo;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingDailyPriceInfoRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.service.ListingService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingDailyPriceInfoRepository dailyPriceInfoRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ListingService listingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getListings_ShouldReturnListOfDtos() {
        // Mock podaci za Stock
        Exchange exchange = new Exchange();
        exchange.setMic("XNAS");

        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");
        stock.setPrice(new BigDecimal("150.50"));
        stock.setExchange(exchange);

        ListingDailyPriceInfo dailyInfo = new ListingDailyPriceInfo();
        dailyInfo.setChange(new BigDecimal("2.50"));
        dailyInfo.setVolume(2000000L);

        ListingDto expectedDto = new ListingDto(
                1L, ListingType.STOCK, "AAPL", new BigDecimal("150.50"), new BigDecimal("2.50"), 2000000L,
                new BigDecimal("165.55"), "XNAS"
        );

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

    @Test
    void getListingDetails_ShouldReturnListingDetailsDto() {
        // Mock podaci za Stock
        Exchange exchange = new Exchange();
        exchange.setMic("XNAS");

        Stock stock = new Stock();
        stock.setId(1L);
        stock.setTicker("AAPL");
        stock.setName("Apple Inc.");
        stock.setPrice(new BigDecimal("150.50"));
        stock.setExchange(exchange);

        ListingDailyPriceInfo dailyInfo1 = new ListingDailyPriceInfo();
        dailyInfo1.setDate(LocalDate.of(2024, 3, 1));
        dailyInfo1.setPrice(new BigDecimal("150.00"));

        ListingDailyPriceInfo dailyInfo2 = new ListingDailyPriceInfo();
        dailyInfo2.setDate(LocalDate.of(2024, 3, 2));
        dailyInfo2.setPrice(new BigDecimal("152.00"));

        List<ListingDailyPriceInfo> priceHistory = List.of(dailyInfo2, dailyInfo1);

        ListingDetailsDto expectedDto = new ListingDetailsDto(
                1L, ListingType.STOCK, "AAPL", "Apple Inc.", new BigDecimal("150.50"), "XNAS",
                List.of(new PriceHistoryDto(LocalDate.of(2024, 3, 2), new BigDecimal("152.00")),
                        new PriceHistoryDto(LocalDate.of(2024, 3, 1), new BigDecimal("150.00"))),
                null, null
        );

        // Mock ponašanje repozitorijuma
        when(listingRepository.findById(1L)).thenReturn(Optional.of(stock));
        when(dailyPriceInfoRepository.findAllByListingOrderByDateDesc(stock)).thenReturn(priceHistory);
        when(listingMapper.toDetailsDto(stock, priceHistory)).thenReturn(expectedDto);

        // Poziv metode
        ListingDetailsDto result = listingService.getListingDetails(1L);

        // Provera rezultata
        assertEquals(expectedDto.getId(), result.getId());
        assertEquals(expectedDto.getTicker(), result.getTicker());
        assertEquals(expectedDto.getCurrentPrice(), result.getCurrentPrice());
        assertEquals(expectedDto.getExchangeMic(), result.getExchangeMic());
        assertEquals(expectedDto.getPriceHistory().size(), result.getPriceHistory().size());

        // Verifikacija poziva
        verify(listingRepository, times(1)).findById(1L);
        verify(dailyPriceInfoRepository, times(1)).findAllByListingOrderByDateDesc(stock);
        verify(listingMapper, times(1)).toDetailsDto(stock, priceHistory);
    }

    @Test
    void getListingDetails_ShouldThrowListingNotFoundException() {
        // Mock ponašanje repozitorijuma - ne postoji listing sa tim ID-em
        when(listingRepository.findById(2L)).thenReturn(Optional.empty());

        // Provera da li baca ListingNotFoundException
        Exception exception = assertThrows(ListingNotFoundException.class, () -> {
            listingService.getListingDetails(2L);
        });

        assertEquals("Listing with ID 2 not found.", exception.getMessage());

        // Verifikacija da je repozitorijum pozvan samo jednom
        verify(listingRepository, times(1)).findById(2L);
        verifyNoInteractions(dailyPriceInfoRepository);
        verifyNoInteractions(listingMapper);
    }

    @Test
    void updateListing_ShouldUpdateListing_WhenUserIsSupervisor() {
        Long listingId = 1L;
        String fakeToken = "Bearer faketoken";

        ListingUpdateDto updateDto = new ListingUpdateDto(
                new BigDecimal("155.00"),
                new BigDecimal("156.00")
        );

        Stock listing = new Stock();
        listing.setId(listingId);
        listing.setPrice(new BigDecimal("150.00"));
        listing.setAsk(new BigDecimal("151.00"));

        ListingDailyPriceInfo dailyInfo = new ListingDailyPriceInfo();
        dailyInfo.setChange(new BigDecimal("2.50"));
        dailyInfo.setVolume(2000000L);

        ListingDto expectedDto = new ListingDto(
                listingId, ListingType.STOCK, "AAPL", new BigDecimal("155.00"), new BigDecimal("2.50"), 2000000L,
                new BigDecimal("156.00"), "XNAS"
        );

        // ✅ Simuliramo da je korisnik SUPERVISOR
        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("SUPERVISOR");

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(dailyPriceInfoRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(dailyInfo);
        when(listingMapper.toDto(listing, dailyInfo)).thenReturn(expectedDto);

        ListingDto result = listingService.updateListing(listingId, updateDto, fakeToken);

        assertEquals(expectedDto.getPrice(), result.getPrice());
        assertEquals(expectedDto.getAsk(), result.getAsk());

        verify(listingRepository, times(1)).findById(listingId);
        verify(listingRepository, times(1)).save(listing);
        verify(listingMapper, times(1)).toDto(listing, dailyInfo);
        verify(jwtTokenUtil, times(1)).getUserRoleFromAuthHeader(fakeToken); // ✅ Provera da je JWT validiran
    }

    @Test
    void updateListing_ShouldThrowUnauthorizedException_WhenUserIsNotSupervisor() {
        Long listingId = 1L;
        String fakeToken = "Bearer faketoken";

        ListingUpdateDto updateDto = new ListingUpdateDto(
                new BigDecimal("155.00"),
                new BigDecimal("156.00")
        );


        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("CLIENT");

        Exception exception = assertThrows(UnauthorizedException.class, () -> {
            listingService.updateListing(listingId, updateDto, fakeToken);
        });

        assertEquals("Only supervisors can update listings.", exception.getMessage());

        verify(jwtTokenUtil, times(1)).getUserRoleFromAuthHeader(fakeToken);
        verifyNoInteractions(listingRepository);
    }

}
