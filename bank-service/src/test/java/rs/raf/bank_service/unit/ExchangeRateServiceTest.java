package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.bank_service.client.ExchangeRateClient;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.dto.UpdateExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.domain.mapper.ExchangeRateMapper;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

    @Mock
    ExchangeRateClient exchangeRateClient;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Currency dummyCurrency1;
    private Currency dummyCurrency2;
    private ExchangeRate dummyExchangeRate;
    private ExchangeRateDto dummyExchangeRateDto;
    private ConvertDto dummyConvertDto;
    private UpdateExchangeRateDto dummyUpdateExchangeRateDto2;

    @BeforeEach
    public void setUp() {
        dummyCurrency1 = new Currency("EUR", "Euro", "€", "EU", "Euro currency", true, "");
        dummyCurrency2 = new Currency("RSD", "Dinar", "RSD", "Serbia", "Dinar currency", true, "");
        dummyExchangeRate = new ExchangeRate(1L, LocalDateTime.now(), dummyCurrency1, dummyCurrency2, BigDecimal.valueOf(117), BigDecimal.valueOf(118));

        dummyExchangeRateDto = ExchangeRateMapper.toDto(dummyExchangeRate);
        dummyConvertDto = new ConvertDto("EUR", "RSD", BigDecimal.valueOf(1));

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", BigDecimal.ONE);
        rates.put("RSD", BigDecimal.valueOf(117));
        dummyUpdateExchangeRateDto2 = new UpdateExchangeRateDto("success", "RSD", rates);
    }

    @Test
    public void testUpdateExchangeRates_Success() {
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateClient.getExchangeRates("RSD")).thenReturn(dummyUpdateExchangeRateDto2);
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(dummyCurrency1));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency2, dummyCurrency1)).thenReturn(Optional.of(dummyExchangeRate));

        exchangeRateService.updateExchangeRates();

        verify(exchangeRateRepository, atLeastOnce()).save(any(ExchangeRate.class));
    }

    @Test
    public void testUpdateExchangeRates_IgnoredInvalidResponse() {
        UpdateExchangeRateDto invalid = new UpdateExchangeRateDto("error", "RSD", null);
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateClient.getExchangeRates("RSD")).thenReturn(invalid);

        exchangeRateService.updateExchangeRates();

        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    public void testUpdateExchangeRates_SkipSameCurrency() {
        Currency same = new Currency("RSD", "Dinar", "RSD", "Serbia", "Dinar", true, "");
        Map<String, BigDecimal> rates = Map.of("RSD", BigDecimal.valueOf(1));
        UpdateExchangeRateDto dto = new UpdateExchangeRateDto("success", "RSD", rates);

        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(same));
        when(exchangeRateClient.getExchangeRates("RSD")).thenReturn(dto);

        exchangeRateService.updateExchangeRates();

        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    public void testGetExchangeRates_Success() {
        when(exchangeRateRepository.findAllActiveExchangeRates()).thenReturn(List.of(dummyExchangeRate));
        List<ExchangeRateDto> result = exchangeRateService.getExchangeRates();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dummyExchangeRateDto.getExchangeRate(), result.get(0).getExchangeRate());
    }

    @Test
    public void testConvert_Success() {
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(dummyCurrency1));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2)).thenReturn(Optional.of(dummyExchangeRate));

        BigDecimal result = exchangeRateService.convert(dummyConvertDto);
        assertEquals(BigDecimal.valueOf(117), result);
    }

    @Test
    public void testConvert_IndirectViaRsd() {
        Currency usd = new Currency("USD", "Dollar", "$", "USA", "Dollar currency", true, "");
        ExchangeRate eurToRsd = new ExchangeRate(null, null, dummyCurrency1, dummyCurrency2, BigDecimal.valueOf(117), BigDecimal.valueOf(118));
        ExchangeRate rsdToUsd = new ExchangeRate(null, null, dummyCurrency2, usd, BigDecimal.valueOf(100), BigDecimal.valueOf(102));

        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(dummyCurrency1));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, usd)).thenReturn(Optional.empty());
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2)).thenReturn(Optional.of(eurToRsd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency2, usd)).thenReturn(Optional.of(rsdToUsd));

        ConvertDto dto = new ConvertDto("EUR", "USD", BigDecimal.ONE);
        BigDecimal result = exchangeRateService.convert(dto);
        assertNotNull(result);
    }

    @Test
    public void testConvert_CurrencyNotFound() {
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.empty());
        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.convert(dummyConvertDto));
    }

    @Test
    public void testConvert_ExchangeRateNotFound() {

        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(dummyCurrency1));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));


        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2))
                .thenReturn(Optional.empty());


        Currency rsd = new Currency("RSD", "Dinar", "RSD", "Serbia", "Dinar currency", true, "");
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(rsd));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, rsd)).thenReturn(Optional.empty());
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, dummyCurrency2)).thenReturn(Optional.empty());


        ExchangeRateNotFoundException exception = assertThrows(ExchangeRateNotFoundException.class, () ->
                exchangeRateService.convert(dummyConvertDto));

        assertEquals("Exchange rate from EUR to RSD not found.", exception.getMessage());
    }


    @Test
    public void testGetExchangeRate_Direct() {
        when(currencyRepository.findByCode(dummyCurrency1.getCode())).thenReturn(Optional.of(dummyCurrency1));
        when(currencyRepository.findByCode(dummyCurrency2.getCode())).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2))
                .thenReturn(Optional.of(dummyExchangeRate));

        ExchangeRateDto result = exchangeRateService.getExchangeRate(dummyCurrency1.getCode(), dummyCurrency2.getCode());

        assertNotNull(result);
        assertEquals(dummyExchangeRate.getExchangeRate(), result.getExchangeRate());
        assertEquals(dummyExchangeRate.getSellRate(), result.getSellRate());

        assertEquals(dummyCurrency1.getCode(), result.getFromCurrency().getCode());
        assertEquals(dummyCurrency2.getCode(), result.getToCurrency().getCode());
    }

    @Test
    public void testGetExchangeRate_NotFound() {
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(dummyCurrency1));
        when(currencyRepository.findByCode("RSD")).thenReturn(Optional.of(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(any(), any())).thenReturn(Optional.empty());

        assertThrows(ExchangeRateNotFoundException.class, () -> exchangeRateService.getExchangeRate("EUR", "RSD"));
    }

    @Test
    public void testGetExchangeRate_CurrencyMissing() {
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.empty());
        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.getExchangeRate("EUR", "RSD"));
    }
}
