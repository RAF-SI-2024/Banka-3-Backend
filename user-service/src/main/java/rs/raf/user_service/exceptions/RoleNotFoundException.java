package rs.raf.user_service.exceptions;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException() {
        super("Role not found with given name.");
    }
}
