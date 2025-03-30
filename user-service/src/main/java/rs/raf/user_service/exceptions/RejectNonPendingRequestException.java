package rs.raf.user_service.exceptions;

public class RejectNonPendingRequestException extends RuntimeException{
    public RejectNonPendingRequestException() {
        super("Cannot reject a non-pending request.");
    }

}
