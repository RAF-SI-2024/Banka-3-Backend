package rs.raf.bank_service.exceptions;

public class InvalidEmploymentStatusException extends RuntimeException{
    public InvalidEmploymentStatusException() {
        super("The provided employment status type is invalid.");
    }

}
