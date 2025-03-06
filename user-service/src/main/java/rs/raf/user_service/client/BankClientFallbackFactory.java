package rs.raf.user_service.client;

import org.springframework.stereotype.Component;

@Component
public class BankClientFallbackFactory implements BankClient {
    @Override
    public void confirmPayment(Long id) {
        throw new RuntimeException("Unable to communicate with Bank Service");
    }

    @Override
    public void confirmTransfer(Long id) {
        throw new RuntimeException("Unable to communicate with Bank Service");
    }

    @Override
    public void changeAccountLimit(Long id) {
        throw new RuntimeException("Unable to communicate with Bank Service");
    }
}
