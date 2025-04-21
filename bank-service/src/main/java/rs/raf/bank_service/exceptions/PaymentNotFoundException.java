package rs.raf.bank_service.exceptions;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found with id: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
