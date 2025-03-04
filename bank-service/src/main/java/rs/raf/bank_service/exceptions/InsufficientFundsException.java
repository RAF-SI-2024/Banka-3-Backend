package rs.raf.bank_service.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(BigDecimal balance, BigDecimal transferAmount) {
        super("Insufficient funds: Available balance " + balance + " is less than transfer amount " + transferAmount);
    }
}
