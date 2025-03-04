package rs.raf.bank_service.exceptions;


public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String id) {
        super("Cannot find currency with id: " + id);

    }

}
