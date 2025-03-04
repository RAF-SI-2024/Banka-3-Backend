package rs.raf.bank_service.exceptions;

public class NotSameCurrencyForTransferException extends RuntimeException {
    public NotSameCurrencyForTransferException(String currency1, String currency2) {
        super("Cannot complete transfer: Accounts have different currencies. " +
                "Sender's currency: " + currency1 + ", Receiver's currency: " + currency2);
    }
}
