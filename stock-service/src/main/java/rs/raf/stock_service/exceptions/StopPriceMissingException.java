package rs.raf.stock_service.exceptions;

import rs.raf.stock_service.domain.enums.OrderType;

public class StopPriceMissingException extends RuntimeException{
    public StopPriceMissingException(OrderType orderType){
        super("Stop price cannot be null for " + orderType + " orders.");
    }
}
