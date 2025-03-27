package rs.raf.bank_service.exceptions;

public class UserNotAClientException extends RuntimeException {
    public UserNotAClientException() {
        super("User sending request is not a client.");
    }
}
