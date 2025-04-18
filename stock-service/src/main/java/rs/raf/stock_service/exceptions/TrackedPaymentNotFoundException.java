package rs.raf.stock_service.exceptions;

public class TrackedPaymentNotFoundException extends RuntimeException {
    public TrackedPaymentNotFoundException() {
        super("Tracked payment not found");
    }
}
