package rs.raf.stock_service.exceptions;

public class SystemPaymentFailedException extends RuntimeException{
    public SystemPaymentFailedException(String message) {
        super(message);
    }
}
