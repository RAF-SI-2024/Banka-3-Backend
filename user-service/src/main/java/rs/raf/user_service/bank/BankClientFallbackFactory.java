package rs.raf.user_service.bank;

import org.springframework.stereotype.Component;
import rs.raf.user_service.dto.RequestConfirmedDto;

@Component
public class BankClientFallbackFactory implements BankClient {

    @Override
    public void confirmPayment(RequestConfirmedDto paymentVerificationRequestDto) {
        // Fallback u slučaju greške
        throw new RuntimeException("Unable to communicate with Bank Service");
    }

    @Override
    public void confirmTransfer(RequestConfirmedDto paymentVerificationRequestDto) {
        throw new RuntimeException("Unable to communicate with Bank Service");
    }
}
