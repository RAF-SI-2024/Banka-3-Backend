package rs.raf.bank_service.exceptions;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String cardNumber) {
        super("Card not found: " + cardNumber);
    }
}
