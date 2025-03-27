package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.raf.stock_service.domain.dto.FuturesContractDto;
import rs.raf.stock_service.domain.entity.FuturesContract;
import rs.raf.stock_service.repository.FuturesRepository;
import rs.raf.stock_service.service.FuturesService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FuturesServiceTest {

    private FuturesRepository futuresRepository;
    private FuturesService futuresService;

    @BeforeEach
    void setUp() {
        futuresRepository = mock(FuturesRepository.class);
        futuresService = new FuturesService(futuresRepository);
    }

    @Test
    void testGetFuturesContractByTicker_returnsDto() {
        FuturesContract fc = new FuturesContract();
        fc.setTicker("WHEAT");
        fc.setContractSize(100);
        fc.setContractUnit("Bushels");
        fc.setMaintenanceMargin(new BigDecimal("3000"));
        fc.setSettlementDate(LocalDate.of(2025, 6, 1));
        fc.setPrice(new BigDecimal("200"));

        when(futuresRepository.findByTicker("WHEAT")).thenReturn(Optional.of(fc));

        FuturesContractDto dto = futuresService.getFuturesContractByTicker("wheat");

        assertEquals("WHEAT", dto.getTicker());
        assertEquals(new BigDecimal("200"), dto.getPrice());
        assertEquals("Bushels", dto.getContractUnit());
    }

    @Test
    void testGetFuturesContractByTicker_invalid_throwsException() {
        when(futuresRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> {
            futuresService.getFuturesContractByTicker("UNKNOWN");
        });

        assertTrue(ex.getMessage().contains("Futures contract not found"));
    }
}
