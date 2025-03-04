package rs.raf.bank_service.exceptions;

public class InvalidTokenException extends RuntimeException{
    public InvalidTokenException(){
        super("The token is invalid.");
    }
}
