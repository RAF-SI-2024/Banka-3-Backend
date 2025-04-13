package rs.raf.stock_service.exceptions;

public class OtcOptionSettlementExpiredException extends RuntimeException{
    public OtcOptionSettlementExpiredException() {
        super("Settlement expired.");
    }

}
