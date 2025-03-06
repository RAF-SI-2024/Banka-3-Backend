package rs.raf.user_service.bank;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.dto.RequestConfirmedDto;

@FeignClient(name = "bank-service", url = "${bank.service.url:http://localhost:8081}", fallbackFactory = BankClientFallbackFactory.class)
public interface BankClient {

    @PostMapping("/api/verification/confirm-payment")
    void confirmPayment(@RequestBody RequestConfirmedDto paymentVerificationRequestDto);

    @PostMapping("/api/verification/confirm-transfer")
    void confirmTransfer(@RequestBody RequestConfirmedDto paymentVerificationRequestDto);
}
