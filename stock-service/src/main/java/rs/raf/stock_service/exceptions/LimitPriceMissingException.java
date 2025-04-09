package rs.raf.stock_service.exceptions;

import rs.raf.stock_service.domain.enums.OrderType;

public class LimitPriceMissingException extends RuntimeException{
    public LimitPriceMissingException(OrderType orderType){
        super("Limit price cannot be null for " + orderType + " orders.");
    }
}
