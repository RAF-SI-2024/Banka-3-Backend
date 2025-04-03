package rs.raf.stock_service.exceptions;

public class PortfolioEntryNotFoundException extends RuntimeException{
    public PortfolioEntryNotFoundException(Long userId, Long listingId) {
        super("Portfolio entry with user ID: " + userId + " and listingId: "+listingId+" not found.");
    }
}
