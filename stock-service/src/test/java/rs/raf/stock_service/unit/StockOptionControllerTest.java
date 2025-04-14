package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import rs.raf.stock_service.controller.StockOptionController;
import rs.raf.stock_service.domain.dto.StockOptionDto;
import rs.raf.stock_service.service.StockOptionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StockOptionControllerTest {

    @Mock
    private StockOptionService stockOptionService;

    @InjectMocks
    private StockOptionController stockOptionsController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getStockOptionsByDate_ShouldReturnStockOptions() {
        Long stockId = 1L;
        LocalDate settlementDate = LocalDate.of(2025, 6, 15);

        StockOptionDto mockDto = StockOptionDto.builder()
                .strikePrice(new BigDecimal("150"))
                .impliedVolatility(new BigDecimal("2.5"))
                .openInterest(100)
                .optionType("CALL")
                .premium(new BigDecimal("10.00"))
                .listingId(55L)
                .build();

        mockDto.setPremium(new BigDecimal("10.00"));
        mockDto.setListingId(55L);

        when(stockOptionService.getStockOptionsByDate(stockId, settlementDate))
                .thenReturn(Collections.singletonList(mockDto));

        ResponseEntity<List<StockOptionDto>> response = stockOptionsController.getStockOptionsByDate(stockId, settlementDate);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
        assertEquals(mockDto, response.getBody().get(0));
        assertEquals(new BigDecimal("10.00"), response.getBody().get(0).getPremium());
        assertEquals(55L, response.getBody().get(0).getListingId());

        verify(stockOptionService, times(1)).getStockOptionsByDate(stockId, settlementDate);
    }

    @Test
    void getStockOptionsByDate_ShouldReturnEmptyListWhenNoOptions() {
        Long stockId = 1L;
        LocalDate settlementDate = LocalDate.of(2025, 6, 15);

        when(stockOptionService.getStockOptionsByDate(stockId, settlementDate))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<StockOptionDto>> response = stockOptionsController.getStockOptionsByDate(stockId, settlementDate);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(0, response.getBody().size());

        verify(stockOptionService, times(1)).getStockOptionsByDate(stockId, settlementDate);
    }
}
