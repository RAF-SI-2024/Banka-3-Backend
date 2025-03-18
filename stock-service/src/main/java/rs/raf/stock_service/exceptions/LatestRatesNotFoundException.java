package rs.raf.stock_service.exceptions;

public class LatestRatesNotFoundException extends RuntimeException {
    public LatestRatesNotFoundException(String message) {
        super(message);
    }
}
