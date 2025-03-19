package rs.raf.bank_service.exceptions;

public class AuthorizedPersonNotFoundException extends RuntimeException {
    private static final String MESSAGE = "Authorized person not found for this company.";

    public AuthorizedPersonNotFoundException() {
        super(MESSAGE);
    }
}
