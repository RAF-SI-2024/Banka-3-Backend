package rs.raf.user_service.exceptions;

public class JmbgAlreadyExistsException extends RuntimeException {
    public JmbgAlreadyExistsException() {
        super("User with this jmbg already exists");
    }
}
