package rs.raf.user_service.exceptions;

public class VerificationNotFoundException extends RuntimeException {

    public VerificationNotFoundException(Long paymentId) {
        super("Verification request not found for paymentId: " + paymentId);
    }
}
