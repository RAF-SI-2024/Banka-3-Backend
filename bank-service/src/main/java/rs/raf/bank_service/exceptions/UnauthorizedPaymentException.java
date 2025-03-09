package rs.raf.bank_service.exceptions;

public class UnauthorizedPaymentException extends RuntimeException {
    public UnauthorizedPaymentException(Long clientId, Long paymentClientId) {
        super("Client with ID " + clientId + " is not authorized to complete this payment. Payment was made for client with ID: " + paymentClientId);
    }
}
