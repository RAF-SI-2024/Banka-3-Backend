package rs.raf.bank_service.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.bootstrap.BootstrapData;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CardRepository;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;
import rs.raf.bank_service.service.ExchangeRateService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BootstrapDataTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private BootstrapData bootstrapData;

    @Test
    void testRun() {
        // Act
        bootstrapData.run();

        // Assert
        verify(currencyRepository, times(1)).saveAll(anyList());
        verify(accountRepository, times(1)).saveAll(anyList());
        verify(cardRepository, times(1)).saveAll(anyList());

        // Očekujemo 2 puta jer imamo dve liste sa exchange rates
        verify(exchangeRateRepository, times(2)).saveAll(anyList());
    }
}
