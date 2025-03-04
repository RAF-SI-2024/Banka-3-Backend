package rs.raf.bank_service.exceptions;

public class ReceiverAccountNotFoundException extends RuntimeException {
    public ReceiverAccountNotFoundException(String id) {
        super("Cannot find receiver account with id: " + id);
    }
}
