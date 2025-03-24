package rs.raf.bank_service.exceptions;

public class PayeeNotFoundException extends RuntimeException {

    public PayeeNotFoundException(Long id) {
        super("Cannot find payee with id: " + id);
    }
}