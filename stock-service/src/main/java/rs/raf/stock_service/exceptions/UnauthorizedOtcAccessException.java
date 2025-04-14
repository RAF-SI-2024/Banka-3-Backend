package rs.raf.stock_service.exceptions;

public class UnauthorizedOtcAccessException extends RuntimeException{
    public UnauthorizedOtcAccessException() {
        super("Only buyer can exercise this option.");
    }
}
