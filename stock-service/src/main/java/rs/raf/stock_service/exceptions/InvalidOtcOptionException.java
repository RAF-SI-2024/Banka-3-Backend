package rs.raf.stock_service.exceptions;

public class InvalidOtcOptionException extends RuntimeException{
    public InvalidOtcOptionException(String message) {
        super(message);
    }
}
