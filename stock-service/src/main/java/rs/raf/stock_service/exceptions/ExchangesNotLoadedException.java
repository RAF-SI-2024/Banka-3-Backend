package rs.raf.stock_service.exceptions;

public class ExchangesNotLoadedException extends RuntimeException {

    public ExchangesNotLoadedException() {
        super("Exceptions not loaded properly.");
    }
}
