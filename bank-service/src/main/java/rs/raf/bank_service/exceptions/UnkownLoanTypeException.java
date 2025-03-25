package rs.raf.bank_service.exceptions;

public class UnkownLoanTypeException extends RuntimeException {
    public UnkownLoanTypeException() {
        super("Unkown loan type");
    }
}
