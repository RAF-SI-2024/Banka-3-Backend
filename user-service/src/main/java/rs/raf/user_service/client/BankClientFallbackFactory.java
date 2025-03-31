package rs.raf.user_service.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class BankClientFallbackFactory implements FallbackFactory<BankClient> {
    @Override
    public BankClient create(Throwable cause) {
        return new BankClient() {
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

            @Override
            public void approveCardRequest(Long id) {
                throw new RuntimeException("Unable to communicate with Bank Service");
            }

            @Override
            public void rejectConfirmPayment(Long id) {
                throw new RuntimeException("Unable to communicate with Bank Service");
            }

            @Override
            public void rejectConfirmTransfer(Long id) {
                throw new RuntimeException("Unable to communicate with Bank Service");
            }

            @Override
            public void rejectChangeAccountLimit(Long id) {
                throw new RuntimeException("Unable to communicate with Bank Service");
            }

            @Override
            public void rejectApproveCardRequest(Long id) {
                throw new RuntimeException("Unable to communicate with Bank Service");
            }
        };
    }
}
