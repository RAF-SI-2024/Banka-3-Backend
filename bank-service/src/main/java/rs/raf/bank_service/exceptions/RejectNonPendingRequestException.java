package rs.raf.bank_service.exceptions;

public class RejectNonPendingRequestException extends RuntimeException{
    public RejectNonPendingRequestException() {
        super("Cannot reject a non-pending request.");
    }

}
