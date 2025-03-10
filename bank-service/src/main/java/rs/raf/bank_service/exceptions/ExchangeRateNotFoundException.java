package rs.raf.bank_service.exceptions;


public class ExchangeRateNotFoundException extends RuntimeException {
    public ExchangeRateNotFoundException(String fromCurrency, String toCurrency) {
        super("Exchange rate not found for " + fromCurrency + " to " + toCurrency);
    }
}
