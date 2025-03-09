package rs.raf.bank_service.exceptions;

public class PaymentCodeNotProvidedException extends RuntimeException {
    public PaymentCodeNotProvidedException() {
        super("Payment code is required and cannot be empty.");
    }
}
