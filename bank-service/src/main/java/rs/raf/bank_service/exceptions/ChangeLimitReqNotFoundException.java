package rs.raf.bank_service.exceptions;

public class ChangeLimitReqNotFoundException extends RuntimeException{
    public ChangeLimitReqNotFoundException(Long requestId) {
        super("Change limit request not found with id: " + requestId);
    }
}
