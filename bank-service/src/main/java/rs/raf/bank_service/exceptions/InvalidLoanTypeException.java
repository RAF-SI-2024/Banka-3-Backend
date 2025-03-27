package rs.raf.bank_service.exceptions;

public class InvalidLoanTypeException extends RuntimeException {
    public InvalidLoanTypeException() {
        super("The provided loan type is invalid.");
    }
}
