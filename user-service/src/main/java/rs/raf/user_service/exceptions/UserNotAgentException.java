package rs.raf.user_service.exceptions;

public class UserNotAgentException extends RuntimeException{
    public UserNotAgentException(Long id) {
        super("User with id: "+id+" is not an agent.");
    }
}
