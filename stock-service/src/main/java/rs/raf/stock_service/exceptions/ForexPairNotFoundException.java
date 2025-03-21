package rs.raf.stock_service.exceptions;

public class ForexPairNotFoundException extends RuntimeException {
    public ForexPairNotFoundException(String message) {
        super(message);
    }
}
