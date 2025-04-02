package rs.raf.stock_service.exceptions;

public class InvalidListingTypeException extends RuntimeException{

    public InvalidListingTypeException(String message){
        super(message);
    }
}
