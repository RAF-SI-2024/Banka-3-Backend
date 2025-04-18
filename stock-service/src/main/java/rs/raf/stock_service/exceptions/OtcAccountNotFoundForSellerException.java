package rs.raf.stock_service.exceptions;

public class OtcAccountNotFoundForSellerException extends RuntimeException {
    public OtcAccountNotFoundForSellerException() {
        super("Account for seller not found");
    }
}
