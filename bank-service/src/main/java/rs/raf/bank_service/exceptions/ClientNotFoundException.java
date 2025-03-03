package rs.raf.bank_service.exceptions;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long id) {
        super("Cannot find client with id: "+id);
    }
}
