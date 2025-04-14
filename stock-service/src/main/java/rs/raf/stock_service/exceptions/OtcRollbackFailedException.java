package rs.raf.stock_service.exceptions;

public class OtcRollbackFailedException extends RuntimeException{
    public OtcRollbackFailedException(String message) {
        super(message);
    }
}
