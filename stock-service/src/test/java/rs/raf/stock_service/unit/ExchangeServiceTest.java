package rs.raf.stock_service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.stock_service.domain.entity.Country;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.exceptions.ExchangesNotLoadedException;
import rs.raf.stock_service.repository.CountryRepository;
import rs.raf.stock_service.repository.ExchangeRepository;
import rs.raf.stock_service.service.ExchangeService;

import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private ExchangeService exchangeService;

    private Exchange exchange1;
    private Exchange exchange2;

    @BeforeEach
    void setUp() {
        Country country = new Country();
        country.setHolidays(Collections.emptyList());
        country.setOpenTime(LocalTime.of(9, 0));
        country.setCloseTime(LocalTime.of(17, 0));

        exchange1 = new Exchange();
        exchange1.setTestMode(false);
        exchange1.setPolity(country);
        exchange1.setTimeZone(0L);

        exchange2 = new Exchange();
        exchange2.setTestMode(true);
        exchange2.setPolity(country);
        exchange2.setTimeZone(0L);
    }

    @Test
    void toggleTestMode_ShouldThrowException_WhenNoExchangesLoaded() {
        when(exchangeRepository.findAll()).thenReturn(Collections.emptyList());
        assertThrows(ExchangesNotLoadedException.class, () -> exchangeService.toggleTestMode());
    }

    @Test
    void toggleTestMode_ShouldToggleTestMode_ForAllExchanges() {
        List<Exchange> exchanges = Arrays.asList(exchange1, exchange2);
        when(exchangeRepository.findAll()).thenReturn(exchanges);

        exchangeService.toggleTestMode();

        assertTrue(exchange1.isTestMode());
        assertFalse(exchange2.isTestMode());
        verify(exchangeRepository, times(1)).findAll();
        verify(exchangeRepository, times(1)).save(exchange1);
        verify(exchangeRepository, times(1)).save(exchange2);
    }



    @Test
    void getAvailableExchanges_ShouldThrowException_WhenNoExchangesLoaded() {
        when(exchangeRepository.findAll()).thenReturn(Collections.emptyList());
        assertThrows(ExchangesNotLoadedException.class, () -> exchangeService.getAvailableExchanges());
    }

    @Test
    void getAvailableExchanges_ShouldReturnTestModeExchanges() {
        when(exchangeRepository.findAll()).thenReturn(Arrays.asList(exchange1, exchange2));
        List<Exchange> result = exchangeService.getAvailableExchanges();
        assertTrue(result.contains(exchange2));
        assertFalse(result.contains(exchange1));
    }

}
