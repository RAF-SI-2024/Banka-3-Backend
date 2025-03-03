package rs.raf.bank_service.exceptions;

public class InvalidCardLimitException extends RuntimeException{
    public InvalidCardLimitException(){
        super("Provided card limit is invalid. Make sure the card limit is a positive value.");
    }
}
