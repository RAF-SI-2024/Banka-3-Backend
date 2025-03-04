package rs.raf.bank_service.exceptions;

public class SendersAccountsCurencyIsNotDinarException extends RuntimeException {
    public SendersAccountsCurencyIsNotDinarException() {
        super("Sender's account currency is not RSD (Dinar). Payment can only be done with RSD.");
    }
}
