package rs.raf.stock_service.exceptions;

public class OrderStatusNotFoundException extends RuntimeException{
    public OrderStatusNotFoundException() {
        super("Status not found.");
    }
}
