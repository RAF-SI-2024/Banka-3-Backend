package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.configuration.ExternalClientConfig;
import rs.raf.bank_service.domain.dto.Bank2AccountListDto;
import rs.raf.bank_service.domain.dto.Bank2TransactionCodeListDto;
import rs.raf.bank_service.domain.dto.ExternalPaymentCreateDto;
import rs.raf.bank_service.domain.dto.ExternalPaymentResponseDto;

@FeignClient(name = "bank2-service", url = "${spring.cloud.openfeign.client.config.bank2.api.url}", configuration = ExternalClientConfig.class)
public interface Bank2Client {

    @PutMapping("/api/v1/transactions/{id}/status")
    Boolean notifySuccess(@PathVariable("id") String id);

    @PostMapping("/api/v1/transactions/")
    ExternalPaymentResponseDto sendExternalPayment(@RequestBody ExternalPaymentCreateDto request);

    @GetMapping("/api/v1/accounts?number={accountNumber}")
    Bank2AccountListDto getAccountDetailsByNumber(@PathVariable("accountNumber") String accountNumber);

    @GetMapping("/api/v1/transactions/codes?code=289")
    Bank2TransactionCodeListDto getTransactionCodeDetails();
}