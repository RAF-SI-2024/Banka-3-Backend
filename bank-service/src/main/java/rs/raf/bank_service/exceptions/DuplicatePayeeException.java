package rs.raf.bank_service.exceptions;

public class DuplicatePayeeException extends RuntimeException {

    public DuplicatePayeeException(String accountNumber) {
        super("Payee with account number " + accountNumber + " already exists");
    }
}
