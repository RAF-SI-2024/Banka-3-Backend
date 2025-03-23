package rs.raf.stock_service.exceptions;

public class CantApproveNonPendingOrder extends RuntimeException{
    public CantApproveNonPendingOrder(Long id) {
        super("Order with ID " + id + " is not in pending status.");
    }

}
