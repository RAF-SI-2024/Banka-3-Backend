package rs.raf.stock_service.exceptions;

public class StocksNotFoundException extends RuntimeException{
    public StocksNotFoundException(String message) {
        super(message);
    }
}
