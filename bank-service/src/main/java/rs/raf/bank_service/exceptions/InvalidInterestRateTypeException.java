package rs.raf.bank_service.exceptions;

public class InvalidInterestRateTypeException extends RuntimeException {
    public InvalidInterestRateTypeException() {
        super("The provided interest rate type is invalid.");
    }
}
