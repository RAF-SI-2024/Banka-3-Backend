package rs.raf.stock_service.exceptions;

public class TransactionNotFoundException extends RuntimeException{
    public TransactionNotFoundException(Long id) {
        super("Transaction with ID " + id + " not found.");
    }
}
