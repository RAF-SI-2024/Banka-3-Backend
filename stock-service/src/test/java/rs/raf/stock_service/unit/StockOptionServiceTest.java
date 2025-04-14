package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.stock_service.domain.dto.StockOptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.domain.mapper.StockOptionMapper;
import rs.raf.stock_service.repository.OptionRepository;
import rs.raf.stock_service.service.StockOptionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class StockOptionServiceTest {

    @Mock
    private OptionRepository optionRepository;

    @Mock
    private StockOptionMapper stockOptionMapper;

    @InjectMocks
    private StockOptionService stockOptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getStockOptionsByDate_ShouldReturnStockOptions() {
        Long stockId = 1L;
        LocalDate settlementDate = LocalDate.of(2025, 6, 15);

        Option mockOption = new Option();
        mockOption.setStrikePrice(new BigDecimal("150"));
        mockOption.setImpliedVolatility(new BigDecimal("2.5"));
        mockOption.setOpenInterest(100);
        mockOption.setOptionType(OptionType.CALL);
        mockOption.setSettlementDate(settlementDate);
        mockOption.setOnSale(true);

        StockOptionDto expectedDto = StockOptionDto.builder()
                .strikePrice(new BigDecimal("150"))
                .impliedVolatility(new BigDecimal("2.5"))
                .openInterest(100)
                .optionType("CALL")
                .premium(new BigDecimal("10.00"))
                .listingId(99L)
                .build();

        when(optionRepository.findByUnderlyingStockIdAndSettlementDate(stockId, settlementDate))
                .thenReturn(Collections.singletonList(mockOption));
        when(stockOptionMapper.toDto(mockOption)).thenReturn(expectedDto);

        List<StockOptionDto> result = stockOptionService.getStockOptionsByDate(stockId, settlementDate);

        assertEquals(1, result.size());
        assertEquals(expectedDto, result.get(0));

        verify(optionRepository, times(1)).findByUnderlyingStockIdAndSettlementDate(stockId, settlementDate);
        verify(stockOptionMapper, times(1)).toDto(mockOption);
    }

    @Test
    void getStockOptionsByDate_ShouldReturnEmptyListWhenNoOptions() {
        Long stockId = 1L;
        LocalDate settlementDate = LocalDate.of(2025, 6, 15);

        when(optionRepository.findByUnderlyingStockIdAndSettlementDate(stockId, settlementDate))
                .thenReturn(Collections.emptyList());

        List<StockOptionDto> result = stockOptionService.getStockOptionsByDate(stockId, settlementDate);

        assertEquals(0, result.size());

        verify(optionRepository, times(1)).findByUnderlyingStockIdAndSettlementDate(stockId, settlementDate);
        verifyNoInteractions(stockOptionMapper);
    }
}
