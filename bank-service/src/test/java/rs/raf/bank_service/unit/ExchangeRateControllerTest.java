package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.bank_service.controller.AccountController;
import rs.raf.bank_service.controller.ExchangeRateController;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.UpdateExchangeRateDto;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
import rs.raf.bank_service.service.ExchangeRateService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@WebMvcTest(ExchangeRateController.class)
public class ExchangeRateControllerTest {

    @MockBean
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController exchangeRateController;

    private ConvertDto dummyConvertDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        dummyConvertDto = new ConvertDto("EUR", "RSD", BigDecimal.valueOf(1));
    }

    @Test
    void testGetExchangeRates_Success() {
        ResponseEntity<?> response = exchangeRateController.getExchangeRates();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    void testGetExchangeRates_Failure() {
        doThrow(new RuntimeException()).when(exchangeRateService).getExchangeRates();

        ResponseEntity<?> response = exchangeRateController.getExchangeRates();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(exchangeRateService).getExchangeRates();
    }

    @Test
    void testConvert_Success() {
        ResponseEntity<?> response = exchangeRateController.convert(dummyConvertDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


    @Test
    void testConvert_Failure() {
        doThrow(new RuntimeException()).when(exchangeRateService).convert(dummyConvertDto);

        ResponseEntity<?> response = exchangeRateController.convert(dummyConvertDto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(exchangeRateService).convert(dummyConvertDto);
    }

    @Test
    void testConvert_NotFound() {
        doThrow(new CurrencyNotFoundException("EUR")).when(exchangeRateService).convert(dummyConvertDto);

        ResponseEntity<?> response = exchangeRateController.convert(dummyConvertDto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(exchangeRateService).convert(dummyConvertDto);
    }
}
