package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import rs.raf.stock_service.controller.ListingController;
import rs.raf.stock_service.domain.dto.*;
import rs.raf.stock_service.domain.enums.ListingType;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.service.ListingService;
import rs.raf.stock_service.utils.JwtTokenUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class ListingControllerTest {

    @Mock
    private ListingService listingService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private ListingController listingController;

    private MockMvc mockMvc;

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
                ListingType.STOCK,
                "AAPL",
                new BigDecimal("150.50"),
                new BigDecimal("2.50"),
                2000000L,
                new BigDecimal("165.55"),
                "XNAS"
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
                ListingType.STOCK,
                "OIL2025",
                new BigDecimal("75.20"),
                new BigDecimal("-0.80"),
                500000L,
                new BigDecimal("82.72"),
                "XNAS"
        );

        when(listingService.getListings(any(ListingFilterDto.class), eq("CLIENT")))
                .thenReturn(Collections.singletonList(mockDto));

        // ✅ Sada LocalDate radi jer je importovan
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



    @Test
    void getListingDetails_ShouldReturnNotFoundWhenListingDoesNotExist() {
        Long nonExistentId = 2L;

        when(listingService.getListingDetails(nonExistentId))
                .thenThrow(new ListingNotFoundException(nonExistentId));

        Exception exception = assertThrows(ListingNotFoundException.class, () -> {
            listingController.getListingDetails(nonExistentId);
        });

        assertEquals("Listing with ID 2 not found.", exception.getMessage());

        // Verifikacija poziva
        verify(listingService, times(1)).getListingDetails(nonExistentId);
    }

    @Test
    void updateListing_ShouldAllowSupervisorToUpdate() {
        Long listingId = 1L;
        ListingUpdateDto updateDto = new ListingUpdateDto(
                new BigDecimal("155.00"),
                new BigDecimal("156.00")
        );

        ListingDto mockDto = new ListingDto(
                listingId, ListingType.STOCK, "AAPL", new BigDecimal("155.00"),
                new BigDecimal("2.50"), 2000000L,
                new BigDecimal("156.00"), "XNAS"
        );

        String fakeToken = "Bearer faketoken";
        when(jwtTokenUtil.getUserRoleFromAuthHeader(fakeToken)).thenReturn("SUPERVISOR");
        when(listingService.updateListing(eq(listingId), any(ListingUpdateDto.class), eq(fakeToken)))
                .thenReturn(mockDto);

        ResponseEntity<ListingDto> response = listingController.updateListing(fakeToken, listingId, updateDto);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockDto, response.getBody());

        verify(listingService, times(1)).updateListing(eq(listingId), any(ListingUpdateDto.class), eq(fakeToken));
    }

}
