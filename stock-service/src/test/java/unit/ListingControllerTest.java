package unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.controller.ListingController;
import rs.raf.stock_service.domain.dto.BuyListingDto;
import rs.raf.stock_service.exceptions.ListingNotFoundException;
import rs.raf.stock_service.service.ListingService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListingControllerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private ListingController listingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void buy_ShouldReturnOk_WhenOrderIsPlacedSuccessfully() {
        BuyListingDto buyListingDto = new BuyListingDto();
        buyListingDto.setListingId(1); // Dodato da izbegnemo NullPointerException
        String authHeader = "Bearer test-token";

        ResponseEntity<?> response = listingController.buy(authHeader, buyListingDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(listingService).placeBuyOrder(any(BuyListingDto.class), eq(authHeader));
    }

    @Test
    void buy_ShouldReturnNotFound_WhenListingNotFound() {
        BuyListingDto buyListingDto = new BuyListingDto();
        buyListingDto.setListingId(1); // Ispravljeno na Long
        String authHeader = "Bearer test-token";

        doThrow(new ListingNotFoundException(Long.valueOf(buyListingDto.getListingId())))
                .when(listingService).placeBuyOrder(any(BuyListingDto.class), anyString());

        ResponseEntity<?> response = listingController.buy(authHeader, buyListingDto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("not found")); // Fleksibilnija provera poruke
        verify(listingService).placeBuyOrder(any(BuyListingDto.class), eq(authHeader));
    }
}
