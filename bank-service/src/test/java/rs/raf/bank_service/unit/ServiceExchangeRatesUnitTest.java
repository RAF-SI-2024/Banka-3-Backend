package rs.raf.bank_service.unit;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceExchangeRatesUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Currency eur;
    private Currency rsd;
    private Currency usd;

    @BeforeEach
    void setUp() {
        eur = new Currency("EUR");
        rsd = new Currency("RSD");
        usd = new Currency("USD");
    }


    /**
     * ✅ Testiranje dohvatanja postojećeg kursa koristeći prodajni kurs.
     */
    @Test
    void testGetExchangeRate_Success_WithSellRate() {
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));

        ExchangeRate exchangeRate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("117.5"), new BigDecimal("118.0"));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(exchangeRate));

        ExchangeRateDto result = exchangeRateService.getExchangeRate("RSD", "EUR");

        assertNotNull(result);
        assertEquals(new BigDecimal("117.5"), result.getExchangeRate());  // Koristi prodajni kurs bez dodatne provizije
    }

    /**
     * ✅ Testiranje kada kurs ne postoji direktno i koristi RSD kao međukorak.
     */
    @Test
    void testGetExchangeRate_UsesIntermediateRSD() {
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(usd, eur))
                .thenReturn(Optional.empty());

        ExchangeRate usdToRsd = new ExchangeRate(null, null, usd, rsd, new BigDecimal("108.0"), new BigDecimal("109.0"));
        ExchangeRate rsdToEur = new ExchangeRate(null, null, rsd, eur, new BigDecimal("0.0085"), new BigDecimal("0.0086"));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(usd, rsd))
                .thenReturn(Optional.of(usdToRsd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(rsdToEur));

        ExchangeRateDto result = exchangeRateService.getExchangeRate("USD", "EUR");

        BigDecimal expectedValue = new BigDecimal("0.93740");  // 109.0 * 0.0086
        assertNotNull(result);
        assertEquals(expectedValue, result.getExchangeRate());
    }

    /**
     * ✅ Testiranje kada kurs ne postoji.
     */
    @Test
    void testGetExchangeRate_NotFound() {
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.empty());

        assertThrows(ExchangeRateNotFoundException.class, () -> {
            exchangeRateService.getExchangeRate("RSD", "EUR");
        });
    }

    /**
     * ✅ Testiranje konverzije sa prodajnim kursom.
     */
    @Test
    void testConvert_Success() {
        ConvertDto convertDto = new ConvertDto("RSD", "EUR", new BigDecimal("1000"));

        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));

        ExchangeRate exchangeRate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("0.0085"), new BigDecimal("0.0086"));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(exchangeRate));

        BigDecimal result = exchangeRateService.convert(convertDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("8.500000").setScale(4, RoundingMode.HALF_UP), result); // 1000 * 0.0086
    }

    /**
     * ✅ Testiranje kada konverzija nije moguća (nema kursa).
     */
    @Test
    void testConvert_ExchangeRateNotFound() {
        ConvertDto convertDto = new ConvertDto("RSD", "GBP", new BigDecimal("1000"));

        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("GBP")).thenReturn(Optional.of(new Currency("GBP")));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ExchangeRateNotFoundException.class, () -> exchangeRateService.convert(convertDto));
    }
}
