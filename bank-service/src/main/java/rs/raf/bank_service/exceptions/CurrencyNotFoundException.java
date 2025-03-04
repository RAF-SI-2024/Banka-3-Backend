package rs.raf.bank_service.exceptions;


public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String currency) {
        super("Currency not found: " + currency);
    }
}