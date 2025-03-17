package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.controller.ListingController;
import rs.raf.stock_service.domain.dto.ListingDto;
import rs.raf.stock_service.domain.dto.ListingFilterDto;
import rs.raf.stock_service.service.ListingService;
import java.time.LocalDate;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListingControllerTest {

    @Mock
    private ListingService listingService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ListingController listingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getListings_ShouldReturnListOfDtos() {
        // Mock JWT Token
        String fakeToken = "Bearer faketoken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("CLIENT");

        // Mock DTO podaci
        ListingDto mockDto = new ListingDto(
                1L,
                "AAPL",
                new BigDecimal("150.50"),
                new BigDecimal("2.50"),
                2000000L,
                new BigDecimal("165.55")
        );

        when(listingService.getListings(any(ListingFilterDto.class), eq("CLIENT")))
                .thenReturn(Collections.singletonList(mockDto));

        // Poziv metode
        ResponseEntity<List<ListingDto>> response = listingController.getListings(
                fakeToken, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, "price", "asc"
        );

        // Provera rezultata
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(mockDto, response.getBody().get(0));

        // Verifikacija poziva
        verify(listingService, times(1)).getListings(any(ListingFilterDto.class), eq("CLIENT"));
    }

    @Test
    void getListings_WithFilters_ShouldReturnFilteredResults() {
        // Mock JWT Token
        String fakeToken = "Bearer faketoken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("CLIENT");

        // Mock DTO podaci
        ListingDto mockDto = new ListingDto(
                2L,
                "OIL2025",
                new BigDecimal("75.20"),
                new BigDecimal("-0.80"),
                500000L,
                new BigDecimal("82.72")
        );

        when(listingService.getListings(any(ListingFilterDto.class), eq("CLIENT")))
                .thenReturn(Collections.singletonList(mockDto));

        // Poziv metode sa filterima
        ResponseEntity<List<ListingDto>> response = listingController.getListings(
                fakeToken, "FUTURES", null, "XNAS", new BigDecimal("50"), new BigDecimal("100"),
                new BigDecimal("74"), new BigDecimal("80"), null, null,
                100000L, 1000000L, null, null, LocalDate.of(2025, 6, 15),
                "volume", "desc"
        );

        // Provera rezultata
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(mockDto, response.getBody().get(0));

        // Verifikacija poziva
        verify(listingService, times(1)).getListings(any(ListingFilterDto.class), eq("CLIENT"));
    }
}
