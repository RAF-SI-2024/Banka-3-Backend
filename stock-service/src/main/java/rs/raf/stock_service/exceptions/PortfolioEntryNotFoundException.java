package rs.raf.stock_service.exceptions;

public class PortfolioEntryNotFoundException extends RuntimeException{
    public PortfolioEntryNotFoundException(String message){
        super(message);
    }
}
