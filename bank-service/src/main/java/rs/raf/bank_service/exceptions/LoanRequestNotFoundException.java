package rs.raf.bank_service.exceptions;

public class LoanRequestNotFoundException extends RuntimeException{
    public LoanRequestNotFoundException() {
        super("The provided loan request doesnt exist.");
    }

}
