package rs.raf.user_service.exceptions;

public class EmployeeNotActive extends RuntimeException {
    public EmployeeNotActive() {
        super("Your account is inactive.");
    }
}
