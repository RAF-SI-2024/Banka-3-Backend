package rs.raf.bank_service.exceptions;

public class PayeesNotFoundByClientIdException extends RuntimeException {
    public PayeesNotFoundByClientIdException(Long clientId) {

        super("Cannot find payee/s with client ID: " + clientId);
    }
}
