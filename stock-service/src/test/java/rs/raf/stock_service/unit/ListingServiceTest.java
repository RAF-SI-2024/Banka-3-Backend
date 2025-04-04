package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.stock_service.client.TwelveDataClient;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.domain.entity.ListingPriceHistory;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.domain.mapper.ListingMapper;
import rs.raf.stock_service.domain.mapper.TimeSeriesMapper;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.exceptions.UnauthorizedException;
import rs.raf.stock_service.repository.ListingPriceHistoryRepository;
import rs.raf.stock_service.repository.ListingRepository;
import rs.raf.stock_service.service.ListingService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private ListingPriceHistoryRepository priceHistoryRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ListingService listingService;

    @Mock
    private TwelveDataClient twelveDataClient;

    @Mock
    private TimeSeriesMapper timeSeriesMapper;


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

        ListingPriceHistory dailyInfo = new ListingPriceHistory();
        dailyInfo.setClose(new BigDecimal("150.00"));
        dailyInfo.setChange(new BigDecimal("2.50"));
        dailyInfo.setVolume(2000000L);

        ListingDto expectedDto = new ListingDto(
                1L, ListingType.STOCK, "AAPL", new BigDecimal("150.50"), new BigDecimal("2.50"), 2000000L,
                new BigDecimal("165.55"), "XNAS"
        );

        // Mock ponašanje repozitorijuma
        when(listingRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(stock));
        when(priceHistoryRepository.findTopByListingOrderByDateDesc(stock)).thenReturn(dailyInfo);
        when(listingMapper.toDto(stock, dailyInfo)).thenReturn(expectedDto);

        // Poziv metode
        List<ListingDto> result = listingService.getListings(new ListingFilterDto(), "CLIENT");

        // Provera rezultata
        assertEquals(1, result.size());
        assertEquals(expectedDto, result.get(0));

        // Verifikacija poziva
        verify(listingRepository, times(1)).findAll(any(Specification.class));
        verify(priceHistoryRepository, times(1)).findTopByListingOrderByDateDesc(stock);
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

        ListingPriceHistory dailyInfo1 = new ListingPriceHistory();
        dailyInfo1.setDate(LocalDateTime.of(2024, 3, 1, 14, 30));
        dailyInfo1.setOpen(new BigDecimal("149.00"));
        dailyInfo1.setHigh(new BigDecimal("151.00"));
        dailyInfo1.setLow(new BigDecimal("148.50"));
        dailyInfo1.setClose(new BigDecimal("150.00"));
        dailyInfo1.setVolume(1500L);

        ListingPriceHistory dailyInfo2 = new ListingPriceHistory();
        dailyInfo2.setDate(LocalDateTime.of(2024, 3, 2, 14, 30));
        dailyInfo2.setOpen(new BigDecimal("151.00"));
        dailyInfo2.setHigh(new BigDecimal("153.00"));
        dailyInfo2.setLow(new BigDecimal("150.50"));
        dailyInfo2.setClose(new BigDecimal("152.00"));
        dailyInfo2.setVolume(2000L);

        List<ListingPriceHistory> priceHistory = List.of(dailyInfo2, dailyInfo1);

        // Očekivani DTO sa novim podacima
        ListingDetailsDto expectedDto = new ListingDetailsDto(
                1L,
                ListingType.STOCK,
                "AAPL",
                "Apple Inc.",
                new BigDecimal("150.50"),
                "XNAS",
                List.of(
                        new PriceHistoryDto(
                                LocalDateTime.of(2024, 3, 2, 14, 30),
                                new BigDecimal("151.00"),
                                new BigDecimal("153.00"),
                                new BigDecimal("150.50"),
                                new BigDecimal("152.00"),
                                2000L
                        ),
                        new PriceHistoryDto(
                                LocalDateTime.of(2024, 3, 1, 14, 30),
                                new BigDecimal("149.00"),
                                new BigDecimal("151.00"),
                                new BigDecimal("148.50"),
                                new BigDecimal("150.00"),
                                1500L
                        )
                ),
                null,
                null,
                List.of(LocalDate.of(2024, 3, 2))
        );

        // Mock ponašanje repozitorijuma
        when(listingRepository.findById(1L)).thenReturn(Optional.of(stock));
        when(priceHistoryRepository.findAllByListingOrderByDateDesc(stock)).thenReturn(priceHistory);
        when(listingMapper.toDetailsDto(stock, priceHistory)).thenReturn(expectedDto);
        when(optionRepository.findAllByUnderlyingStock(stock)).thenReturn(List.of());


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
        verify(priceHistoryRepository, times(1)).findAllByListingOrderByDateDesc(stock);
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
        verifyNoInteractions(priceHistoryRepository);
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

        ListingPriceHistory dailyInfo = new ListingPriceHistory();
        dailyInfo.setChange(new BigDecimal("2.50"));
        dailyInfo.setVolume(2000000L);

        ListingDto expectedDto = new ListingDto(
                listingId, ListingType.STOCK, "AAPL", new BigDecimal("155.00"), new BigDecimal("2.50"), 2000000L,
                new BigDecimal("156.00"), "XNAS"
        );

        // ✅ Simuliramo da je korisnik SUPERVISOR
        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("SUPERVISOR");

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(priceHistoryRepository.findTopByListingOrderByDateDesc(listing)).thenReturn(dailyInfo);
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

    @Test
    void getPriceHistory_ShouldReturnTimeSeriesDto_WhenListingExists() throws Exception {
        // Mock podaci
        Long listingId = 1L;
        String interval = "1day";
        String apiResponse = "{ \"meta\": { \"symbol\": \"AAPL\", \"interval\": \"1day\", \"currency\": \"USD\" }, \"values\": [] }";

        Exchange exchange = new Exchange();
        exchange.setAcronym("XNAS");

        Stock stock = new Stock();
        stock.setId(listingId);
        stock.setTicker("AAPL");
        stock.setExchange(exchange);

        TimeSeriesDto mockDto = new TimeSeriesDto();
        TimeSeriesDto.MetaDto metaDto = new TimeSeriesDto.MetaDto();
        metaDto.setSymbol("AAPL");
        metaDto.setInterval("1day");
        metaDto.setCurrency("USD");
        metaDto.setExchange("XNAS");
        mockDto.setMeta(metaDto);

        // Mock ponašanje
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(stock));
        when(twelveDataClient.getTimeSeries("AAPL", interval, "30")).thenReturn(apiResponse);
        when(timeSeriesMapper.mapJsonToCustomTimeSeries(apiResponse, stock)).thenReturn(mockDto);

        // Poziv metode
        TimeSeriesDto result = listingService.getPriceHistory(listingId, interval);

        // Provera rezultata
        assertEquals("AAPL", result.getMeta().getSymbol());
        assertEquals("1day", result.getMeta().getInterval());
        assertEquals("XNAS", result.getMeta().getExchange());

        // Verifikacija poziva
        verify(listingRepository, times(1)).findById(listingId);
        verify(twelveDataClient, times(1)).getTimeSeries("AAPL", interval, "30");
        verify(timeSeriesMapper, times(1)).mapJsonToCustomTimeSeries(apiResponse, stock);
    }

    @Test
    void getPriceHistory_ShouldThrowListingNotFoundException_WhenListingDoesNotExist() {
        Long listingId = 2L;
        String interval = "1day";

        // Mock ponašanje - listing sa ID 2 ne postoji
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Provera da li baca ListingNotFoundException
        Exception exception = assertThrows(ListingNotFoundException.class, () -> {
            listingService.getPriceHistory(listingId, interval);
        });

        assertEquals("Listing with ID 2 not found.", exception.getMessage());

        // Verifikacija poziva
        verify(listingRepository, times(1)).findById(listingId);
        verifyNoInteractions(twelveDataClient);
        verifyNoInteractions(timeSeriesMapper);
    }

    @Test
    void getPriceHistory_ShouldThrowRuntimeException_WhenApiResponseInvalid() {
        Long listingId = 1L;
        String interval = "1day";
        String invalidApiResponse = "INVALID_JSON_RESPONSE";

        Exchange exchange = new Exchange();
        exchange.setAcronym("XNAS");

        Stock stock = new Stock();
        stock.setId(listingId);
        stock.setTicker("AAPL");
        stock.setExchange(exchange);

        // Mock ponašanje - validan listing, ali neispravan JSON iz API-ja
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(stock));
        when(twelveDataClient.getTimeSeries("AAPL", interval, "30")).thenReturn(invalidApiResponse);
        when(timeSeriesMapper.mapJsonToCustomTimeSeries(invalidApiResponse, stock))
                .thenThrow(new RuntimeException("Error mapping JSON to Time Series DTO"));

        // Provera da li baca RuntimeException
        Exception exception = assertThrows(RuntimeException.class, () -> {
            listingService.getPriceHistory(listingId, interval);
        });

        assertEquals("Error mapping JSON to Time Series DTO", exception.getMessage());

        // Verifikacija poziva
        verify(listingRepository, times(1)).findById(listingId);
        verify(twelveDataClient, times(1)).getTimeSeries("AAPL", interval, "30");
        verify(timeSeriesMapper, times(1)).mapJsonToCustomTimeSeries(invalidApiResponse, stock);
    }


}
