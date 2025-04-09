package rs.raf.bank_service.exceptions;


public class ExternalAccountNotFoundException extends RuntimeException {
    public ExternalAccountNotFoundException(String accountNumber) {
        super("Cannot find external reciever with account number: " + accountNumber);
    }
}
