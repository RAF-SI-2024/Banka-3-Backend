package rs.raf.bank_service.exceptions;

public class LoanNotFoundException extends RuntimeException{
    public LoanNotFoundException() {
        super("Can't find Loan");

    }
}
