package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import rs.raf.bank_service.domain.dto.Bank2AccountDetailsDto;
import rs.raf.bank_service.domain.dto.Bank2AccountListDto;

@FeignClient(name = "bank2-service", url = "${spring.cloud.openfeign.client.config.bank2.api.url}")
public interface Bank2Client {

//    @PostMapping("/api/v1/transactions/")
//    ExternalPaymentResponseDto initiateExternalPayment(@RequestBody ExternalPaymentRequestDto request);

    @GetMapping("/api/v1/accounts?number={accountNumber}")
    Bank2AccountListDto getAccountDetailsByNumber(@PathVariable("accountNumber") String accountNumber);
}