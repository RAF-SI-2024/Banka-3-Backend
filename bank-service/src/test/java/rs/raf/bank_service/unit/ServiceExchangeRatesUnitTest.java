package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.client.ExchangeRateClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceExchangeRatesUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ExchangeRateClient exchangeRateClient;
    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Currency rsd;
    private Currency eur;
    private Currency usd;

    @BeforeEach
    void setup() {
        rsd = new Currency("RSD");
        eur = new Currency("EUR");
        usd = new Currency("USD");
    }

    @Test
    void testUpdateExchangeRates_Success() {
        UpdateExchangeRateDto mockResponse = new UpdateExchangeRateDto();
        mockResponse.setResult("success");
        mockResponse.setConversionRates(Map.of(
                "EUR", new BigDecimal("117.5"),
                "USD", new BigDecimal("108.0")
        ));

        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(exchangeRateClient.getExchangeRates("RSD")).thenReturn(mockResponse);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(any(), any())).thenReturn(Optional.empty());

        exchangeRateService.updateExchangeRates();

        verify(exchangeRateRepository, times(4)).save(any()); // 2 unosa + 2 mirrored
    }

    @Test
    void testUpdateExchangeRates_CurrencyNotFound() {
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.empty());
        exchangeRateService.updateExchangeRates();
        verify(exchangeRateClient, never()).getExchangeRates(any());
    }

    @Test
    void testUpdateExchangeRates_ResponseIsNull() {
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(exchangeRateClient.getExchangeRates("RSD")).thenReturn(null);
        exchangeRateService.updateExchangeRates();
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    void testGetExchangeRates_ReturnsActiveList() {
        ExchangeRate rate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("117.5"), new BigDecimal("118.0"));
        when(exchangeRateRepository.findAllActiveExchangeRates()).thenReturn(List.of(rate));

        List<ExchangeRateDto> result = exchangeRateService.getExchangeRates();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("RSD", result.get(0).getFromCurrency().getCode());
        assertEquals("EUR", result.get(0).getToCurrency().getCode());
    }

    @Test
    void testConvert_Success() {
        ConvertDto dto = new ConvertDto("RSD", "EUR", new BigDecimal("1000"));

        ExchangeRate rate = new ExchangeRate(null, null, rsd, eur, new BigDecimal("0.0085"), new BigDecimal("0.0086"));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eur));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, eur)).thenReturn(Optional.of(rate));

        BigDecimal result = exchangeRateService.convert(dto);

        assertEquals(new BigDecimal("8.5000").setScale(4, RoundingMode.HALF_UP), result);
    }

    @Test
    void testGetExchangeRate_CurrencyNotFound() {
        when(currencyRepository.findByCode("XXX")).thenReturn(Optional.empty());
        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.getExchangeRate("XXX", "EUR"));
    }

    @Test
    void testGetExchangeRate_IntermediateRateUsed() {
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

        assertEquals(new BigDecimal("0.91800"), result.getExchangeRate());
    }
}
