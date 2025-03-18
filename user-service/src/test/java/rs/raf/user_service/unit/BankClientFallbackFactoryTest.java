package rs.raf.user_service.unit;


import org.junit.jupiter.api.Test;
import rs.raf.user_service.client.BankClientFallbackFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BankClientFallbackFactoryTest {

    private final BankClientFallbackFactory fallbackFactory = new BankClientFallbackFactory();

    @Test
    void testConfirmPaymentThrowsException() {
        assertThrows(RuntimeException.class, () -> fallbackFactory.confirmPayment(1L),
                "Expected RuntimeException when confirming payment");
    }

    @Test
    void testConfirmTransferThrowsException() {
        assertThrows(RuntimeException.class, () -> fallbackFactory.confirmTransfer(1L),
                "Expected RuntimeException when confirming transfer");
    }

    @Test
    void testChangeAccountLimitThrowsException() {
        assertThrows(RuntimeException.class, () -> fallbackFactory.changeAccountLimit(1L),
                "Expected RuntimeException when changing account limit");
    }
}
