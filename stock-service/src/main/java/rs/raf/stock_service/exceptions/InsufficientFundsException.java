package rs.raf.stock_service.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal amount) {
        super("Insufficient funds: Available balance is less than expected cost: " + amount);
    }
}
