package rs.raf.bank_service.exceptions;

public class PurposeOfPaymentNotProvidedException extends RuntimeException {
    public PurposeOfPaymentNotProvidedException() {
        super("Purpose of payment is required and cannot be empty.");
    }
}
