package rs.raf.bank_service.exceptions;

public class InvalidLoanStatusException extends RuntimeException {
    public InvalidLoanStatusException() {
        super("The provided loan status is invalid.");
    }

}
