package rs.raf.stock_service.exceptions;

public class FuturesContractNotFoundException extends RuntimeException {
    public FuturesContractNotFoundException(String message) {
        super(message);
    }
}