package rs.raf.stock_service.exceptions;

public class OtcOptionNotFoundException extends RuntimeException{
    public OtcOptionNotFoundException(Long id) {
        super("OTC Option with id " + id + " not found.");
    }
}
