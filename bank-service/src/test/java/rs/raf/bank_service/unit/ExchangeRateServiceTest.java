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
    private UpdateExchangeRateDto dummyUpdateExchangeRateDto1;
    private UpdateExchangeRateDto dummyUpdateExchangeRateDto2;
    private ConvertDto dummyConvertDto;

    @BeforeEach
    public void setUp() {
        dummyCurrency1 = new Currency("EUR", "Euro", "€", "EU", "Euro currency", true);
        dummyCurrency2 = new Currency("RSD", "Dinar", "RSD", "Serbia", "Dinar currency", true);
        dummyExchangeRate = new ExchangeRate(1L, LocalDateTime.now(), dummyCurrency1, dummyCurrency2, BigDecimal.valueOf(117), BigDecimal.valueOf(118));

        dummyExchangeRateDto = ExchangeRateMapper.toDto(dummyExchangeRate);

        Map<String, BigDecimal> dummyConversionRates = new HashMap<>();
        dummyConversionRates.put("RSD", BigDecimal.valueOf(1));
        dummyConversionRates.put("EUR", BigDecimal.valueOf(0.01));
        dummyUpdateExchangeRateDto1 = new UpdateExchangeRateDto("success", "RSD", dummyConversionRates);

        dummyConversionRates = new HashMap<>();
        dummyConversionRates.put("EUR", BigDecimal.valueOf(1));
        dummyConversionRates.put("RSD", BigDecimal.valueOf(100));
        dummyUpdateExchangeRateDto2 = new UpdateExchangeRateDto("success", "EUR", dummyConversionRates);

        dummyConvertDto = new ConvertDto("EUR", "RSD", BigDecimal.valueOf(1));
    }

    @Test
    public void testUpdateExchangeRates_Success() {
        when(exchangeRateClient.getExchangeRates(dummyCurrency2.getCode())).thenReturn(dummyUpdateExchangeRateDto2);
        when(currencyRepository.findByCode(dummyCurrency1.getCode())).thenReturn(Optional.ofNullable(dummyCurrency1));
        when(currencyRepository.findByCode(dummyCurrency2.getCode())).thenReturn(Optional.ofNullable(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency2, dummyCurrency1))
                .thenReturn(Optional.ofNullable(dummyExchangeRate));
        exchangeRateService.updateExchangeRates();
        verify(exchangeRateRepository, times(2)).save(any(ExchangeRate.class));
    }

    @Test
    public void testGetExchangeRates_Success() {
        when(exchangeRateRepository.findAllActiveExchangeRates()).thenReturn(List.of(dummyExchangeRate));

        List<ExchangeRateDto> result = exchangeRateService.getExchangeRates();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dummyExchangeRateDto, result.get(0));
    }

    @Test
    public void testConvert_Success() {
        when(currencyRepository.findByCode(dummyCurrency1.getCode())).thenReturn(Optional.ofNullable(dummyCurrency1));
        when(currencyRepository.findByCode(dummyCurrency2.getCode())).thenReturn(Optional.ofNullable(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2))
                .thenReturn(Optional.ofNullable(dummyExchangeRate));

        BigDecimal result = exchangeRateService.convert(dummyConvertDto);
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(117), result);
    }

    @Test
    public void testConvert_CurrencyNotFound() {
        when(currencyRepository.findByCode(dummyCurrency1.getCode())).thenThrow(new CurrencyNotFoundException(dummyCurrency1.getCode()));

        CurrencyNotFoundException exception = assertThrows(CurrencyNotFoundException.class, () ->
                exchangeRateService.convert(dummyConvertDto));
        assertEquals("Currency not found: " + dummyCurrency1.getCode(), exception.getMessage());
    }

    @Test
    public void testConvert_ExchangeRateNotFound() {
        when(currencyRepository.findByCode(dummyCurrency1.getCode())).thenReturn(Optional.ofNullable(dummyCurrency1));
        when(currencyRepository.findByCode(dummyCurrency2.getCode())).thenReturn(Optional.ofNullable(dummyCurrency2));
        when(exchangeRateRepository.findByFromCurrencyAndToCurrency(dummyCurrency1, dummyCurrency2)).
                thenThrow(new ExchangeRateNotFoundException(dummyCurrency1.getCode(), dummyCurrency2.getCode()));

        ExchangeRateNotFoundException exception = assertThrows(ExchangeRateNotFoundException.class, () ->
                exchangeRateService.convert(dummyConvertDto));
        assertEquals("Exchange rate from " + dummyCurrency1.getCode() + " to " + dummyCurrency2.getCode() + " not found."
                , exception.getMessage());
    }
}
