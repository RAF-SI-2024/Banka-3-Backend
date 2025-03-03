package rs.raf.user_service.exceptions;

public class ActivityCodeNotFoundException extends RuntimeException {
    public ActivityCodeNotFoundException(Long id) {
        super("Cannot find activity code with id: "+id);
    }

}
