package rs.raf.bank_service.exceptions;

public class BankAccountNotFoundException extends RuntimeException {

    public BankAccountNotFoundException(String bank_rsd_account_not_found) {
        super(bank_rsd_account_not_found);
    }
}
