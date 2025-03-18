package rs.raf.stock_service.exceptions;

public class ExchangeRateConversionException extends RuntimeException {
    public ExchangeRateConversionException(String message) {
        super(message);
    }
}
