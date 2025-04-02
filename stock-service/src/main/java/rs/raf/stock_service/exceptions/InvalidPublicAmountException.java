package rs.raf.stock_service.exceptions;

public class InvalidPublicAmountException extends RuntimeException{

    public InvalidPublicAmountException(String message){
        super(message);
    }
}
