package rs.raf.bank_service.exceptions;

public class UnauthorizedTransferConormationException extends RuntimeException {
    public UnauthorizedTransferConormationException(Long clientId, Long paymentClientId) {
        super("Client with ID " + clientId + " is not authorized to complete this transfer. Payment was made for client with ID: " + paymentClientId);
    }
}
