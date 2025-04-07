package rs.raf.stock_service.exceptions;

public class CantCancelOrderInCurrentOrderState  extends RuntimeException {
    public CantCancelOrderInCurrentOrderState(Long id) {
        super("Order with ID " + id + " can't be cancelled in it's current order state.");
    }
}
