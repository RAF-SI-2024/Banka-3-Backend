package rs.raf.stock_service.exceptions;

public class OtcExecutionFailedException extends RuntimeException{
    public OtcExecutionFailedException(String message) {
        super(message);
    }
}
