package rs.raf.bank_service.exceptions;

public class CardLimitExceededException extends RuntimeException {
    public CardLimitExceededException(String account) {
        super("Card limit exceeded for the account with account number: " + account);
    }
}
