package rs.raf.stock_service.exceptions;

public class ForexPairsNotFoundException extends RuntimeException {
    public ForexPairsNotFoundException(String message) {
        super(message);
    }
}
