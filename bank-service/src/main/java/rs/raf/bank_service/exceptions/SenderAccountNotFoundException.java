package rs.raf.bank_service.exceptions;

public class SenderAccountNotFoundException extends RuntimeException {
    public SenderAccountNotFoundException(String id) {
        super("Cannot find sender account with id: " + id);
    }
}
