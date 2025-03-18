package rs.raf.user_service.exceptions;

public class ClientCannotBeAgentException extends RuntimeException {
    public ClientCannotBeAgentException(Long id) {
        super("User with id " + id + " is client, cannot set agent role for clients.");
    }
}
