package rs.raf.bank_service.exceptions;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException() {
        super("Error while communicating with another service.");
    }
}
