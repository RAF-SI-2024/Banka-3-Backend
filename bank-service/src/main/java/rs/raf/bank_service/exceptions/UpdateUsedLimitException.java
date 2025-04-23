package rs.raf.bank_service.exceptions;

public class UpdateUsedLimitException extends RuntimeException {
    public UpdateUsedLimitException() {
        super("Can not place order. Could not update used limit.");
    }
}
