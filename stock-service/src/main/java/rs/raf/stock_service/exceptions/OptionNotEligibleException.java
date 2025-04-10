package rs.raf.stock_service.exceptions;

public class OptionNotEligibleException extends RuntimeException {
    public OptionNotEligibleException(String message) {
        super(message);
    }
}