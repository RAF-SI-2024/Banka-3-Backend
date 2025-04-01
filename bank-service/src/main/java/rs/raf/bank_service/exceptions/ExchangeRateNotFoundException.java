package rs.raf.bank_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExchangeRateNotFoundException extends RuntimeException {
    public ExchangeRateNotFoundException(String fromCurrencyCode, String toCurrencyCode) {
        super("Exchange rate from " + fromCurrencyCode + " to " + toCurrencyCode + " not found.");
    }
}
