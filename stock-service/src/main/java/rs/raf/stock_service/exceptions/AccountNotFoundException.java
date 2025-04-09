package rs.raf.stock_service.exceptions;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNUmber) {
        super("Account " + accountNUmber + " not found");
    }
}
