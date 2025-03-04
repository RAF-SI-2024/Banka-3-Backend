package rs.raf.bank_service.exceptions;

public class InvalidCardTypeException extends RuntimeException {
    public InvalidCardTypeException() {
        super("The provided card type is invalid.");
    }
}
