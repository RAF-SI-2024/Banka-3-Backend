package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.raf.stock_service.domain.dto.OptionDto;
import rs.raf.stock_service.domain.entity.Option;
import rs.raf.stock_service.domain.entity.Stock;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.repository.OptionRepository;
import rs.raf.stock_service.service.OptionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OptionServiceTest {

    private OptionRepository optionRepository;
    private OptionService optionService;

    @BeforeEach
    void setUp() {
        optionRepository = mock(OptionRepository.class);
        optionService = new OptionService(optionRepository);
    }

    @Test
    void testGenerateOptions_returnsCorrectSizeAndData() {
        BigDecimal currentPrice = new BigDecimal("100");
        List<OptionDto> options = optionService.generateOptions("AAPL", currentPrice);

        int expectedStrikes = 11;
        int expectedDates = 12;
        int expectedOptions = expectedStrikes * expectedDates * 2;
        assertEquals(expectedOptions, options.size());


        OptionDto first = options.get(0);
        assertEquals("AAPL", first.getStockListing());
        assertEquals(new BigDecimal("50").multiply(currentPrice), first.getMaintenanceMargin());
    }

    @Test
    void testGenerateOptions_withLowPrice_doesNotBreak() {
        BigDecimal lowPrice = new BigDecimal("0.50");
        List<OptionDto> options = optionService.generateOptions("PENNY", lowPrice);

        assertFalse(options.isEmpty());
    }

    @Test
    void testGetOptionByTicker_validTicker_returnsDto() {
        Stock stock = new Stock();
        stock.setTicker("TSLA");

        Option option = new Option();
        option.setUnderlyingStock(stock);
        option.setOptionType(OptionType.CALL);
        option.setStrikePrice(new BigDecimal("220"));
        option.setContractSize(new BigDecimal("100"));
        option.setSettlementDate(LocalDate.now().plusDays(30));
        option.setMaintenanceMargin(new BigDecimal("11000"));

        when(optionRepository.findByTicker("TSLA_CALL")).thenReturn(Optional.of(option));

        OptionDto dto = optionService.getOptionByTicker("tsla_call");

        assertEquals("TSLA", dto.getStockListing());
        assertEquals(OptionType.CALL, dto.getOptionType());
    }

    @Test
    void testGetOptionByTicker_invalidTicker_throwsException() {
        when(optionRepository.findByTicker("FAKE")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            optionService.getOptionByTicker("FAKE");
        });

        assertTrue(exception.getMessage().contains("Option not found"));
    }

    @Test
    void testGetAllOptions_emptyList_returnsEmpty() {
        when(optionRepository.findAllOptions()).thenReturn(Collections.emptyList());

        List<OptionDto> options = optionService.getAllOptions();

        assertTrue(options.isEmpty());
    }

    @Test
    void testGetOptionsByType_returnsOnlyMatchingType() {
        Stock stock = new Stock();
        stock.setTicker("META");

        Option call = new Option();
        call.setUnderlyingStock(stock);
        call.setOptionType(OptionType.CALL);
        call.setStrikePrice(new BigDecimal("300"));
        call.setContractSize(new BigDecimal("100"));
        call.setMaintenanceMargin(new BigDecimal("8000"));
        call.setSettlementDate(LocalDate.now().plusDays(20));

        when(optionRepository.findByOptionType(OptionType.CALL)).thenReturn(List.of(call));

        List<OptionDto> options = optionService.getOptionsByType(OptionType.CALL);

        assertEquals(1, options.size());
        assertEquals(OptionType.CALL, options.get(0).getOptionType());
    }
}
