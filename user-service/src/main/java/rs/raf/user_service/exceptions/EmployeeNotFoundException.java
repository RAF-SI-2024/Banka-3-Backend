package rs.raf.user_service.exceptions;

public class EmployeeNotFoundException extends RuntimeException{
    public EmployeeNotFoundException(Long id) {
        super("Cannot find employee with id: " + id);
    }
}
