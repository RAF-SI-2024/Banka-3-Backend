package rs.raf.bank_service.exceptions;

public class UnathorizedPersonException extends RuntimeException {
    private static final String MESSAGE = "You do not have permission to assign an authorized person.";


    public UnathorizedPersonException() {
        super(MESSAGE);
    }
}
