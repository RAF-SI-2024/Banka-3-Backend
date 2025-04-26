package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import rs.raf.bank_service.domain.dto.*;

@FeignClient(name = "banka2", url = "${url.banke2}")
public interface Banka2Client {
    @GetMapping("/api/v1/accounts/number/{accountNumber}")
    Bank2AccountDetailsDto getAccountDetailsByNumber(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/v1/payments/confirm")
    Bank2PaymentResponseDto sendPaymentToBank2(@RequestBody InterbankPaymentDto dto);
}
