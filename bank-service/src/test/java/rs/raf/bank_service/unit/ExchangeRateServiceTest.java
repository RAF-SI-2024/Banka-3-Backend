package rs.raf.bank_service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.exceptions.InvalidExchangeRateException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

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
     *  ✅ Testiranje dohvatanja postojećeg kursa sa provizijom
     */
    @Test
    void testGetExchangeRate_Success_WithCommission() {
        //  Simuliramo da valute postoje
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));

        // Simuliramo postojanje direktnog kursa sa EUR -> RSD
        ExchangeRate exchangeRate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("117.5"), null, null);
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(exchangeRate));

        //  Poziv metode i provera rezultata sa provizijom (99% originalnog kursa)
        BigDecimal result = exchangeRateService.getExchangeRate("RSD", "EUR");

        assertNotNull(result);
        assertEquals(new BigDecimal("116.325").setScale(3, RoundingMode.HALF_UP), result);
    }

    /**
     *  ✅ Testiranje kada kurs ne postoji direktno i koristi RSD kao međukorak
     */
    @Test
    void testGetExchangeRate_UsesIntermediateRSD() {
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));

        // Simuliramo da direktan kurs ne postoji
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(usd, eur))
                .thenReturn(Optional.empty());

        // Simuliramo USD -> RSD i RSD -> EUR
        ExchangeRate usdToRsd = new ExchangeRate(null, null, usd, rsd, new BigDecimal("108.0"), null, null);
        ExchangeRate rsdToEur = new ExchangeRate(null, null, rsd, eur, new BigDecimal("0.0085"), null, null);

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(usd, rsd))
                .thenReturn(Optional.of(usdToRsd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(rsdToEur));

        //  Poziv metode
        BigDecimal result = exchangeRateService.getExchangeRate("USD", "EUR");

        //  Postavljamo preciznost na 3 decimale kao što metoda koristi
        BigDecimal expectedValue = new BigDecimal("0.90882").setScale(3, RoundingMode.HALF_UP);
        result = result.setScale(3, RoundingMode.HALF_UP);

        assertNotNull(result);
        assertEquals(expectedValue, result);
    }


    /**
     *  ✅ Testiranje kada kurs ne postoji
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
     *  ✅ Testiranje dodavanja novog kursa sa međukursima (RSD)
     */
    @Test
    void testSaveOrUpdateExchangeRate_WithIntermediateRSD() {
        BigDecimal rate = new BigDecimal("117.5");

        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(usd, eur))
                .thenReturn(Optional.empty());

        when(exchangeRateRepository.save(any(ExchangeRate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRate savedExchangeRate = exchangeRateService.saveOrUpdateExchangeRate("USD", "EUR", rate);

        assertNotNull(savedExchangeRate);
        assertEquals(rate, savedExchangeRate.getExchangeRate());

        // Verifikujemo da se dodaju međukursevi
        verify(exchangeRateRepository, times(1)).save(new ExchangeRate(null, null, usd, rsd, rate.multiply(BigDecimal.valueOf(0.99)), null, null));
        verify(exchangeRateRepository, times(1)).save(new ExchangeRate(null, null, rsd, eur, BigDecimal.ONE.divide(rate.multiply(BigDecimal.valueOf(0.99)), 4, RoundingMode.HALF_UP), null, null));
    }

    /**
     *  ✅ Testiranje ažuriranja postojećeg kursa
     */
    @Test
    void testSaveOrUpdateExchangeRate_UpdateExisting() {
        BigDecimal newRate = new BigDecimal("118.0");
        ExchangeRate existingExchangeRate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("117.5"), null, null);

        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));

        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur))
                .thenReturn(Optional.of(existingExchangeRate));
        when(exchangeRateRepository.save(any(ExchangeRate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRate updatedExchangeRate = exchangeRateService.saveOrUpdateExchangeRate("RSD", "EUR", newRate);

        assertNotNull(updatedExchangeRate);
        assertEquals(newRate, updatedExchangeRate.getExchangeRate());
    }

    /**
     *   Testiranje kada je unet nevalidan kurs
     */
    @Test
    void testSaveOrUpdateExchangeRate_InvalidRate() {
        BigDecimal invalidRate = new BigDecimal("0.0");

        assertThrows(InvalidExchangeRateException.class, () -> {
            exchangeRateService.saveOrUpdateExchangeRate("RSD", "EUR", invalidRate);
        });
    }
}
