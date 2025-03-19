package rs.raf.bank_service.exceptions;

public class InvalidAuthorizedPersonException extends RuntimeException {
    private static final String MESSAGE = "Authorized person does not belong to this company.";

    public InvalidAuthorizedPersonException() {
        super(MESSAGE);
    }
}
