package rs.raf.user_service.exceptions;

public class TaxIdAlreadyExistsException extends RuntimeException {
    public TaxIdAlreadyExistsException(String id) {
        super("TaxId: " + id + " " + "already exits.");
    }

}
