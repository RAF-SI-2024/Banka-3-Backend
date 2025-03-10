package rs.raf.bank_service.exceptions;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(Long id) {
        super("Cannot find loan with id: " + id);
    }
}
