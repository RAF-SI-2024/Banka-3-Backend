package rs.raf.stock_service.exceptions;

public class ActuaryLimitNotFoundException extends RuntimeException{
    public ActuaryLimitNotFoundException(Long id) {
        super("Actuary limit with employeeId: "+id+" is not found.");
    }
}
