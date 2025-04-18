package rs.raf.stock_service.exceptions;

public class OtcAccountNotFoundForBuyerException extends RuntimeException {
    public OtcAccountNotFoundForBuyerException() {
        super("Account for buyer not found");
    }
}
