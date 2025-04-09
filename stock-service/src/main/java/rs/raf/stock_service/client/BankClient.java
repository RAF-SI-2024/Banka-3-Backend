package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.TaxDto;

import java.math.BigDecimal;

/// Klasa koja sluzi za slanje HTTP poziva na bankService
@FeignClient(name = "bank-service", url = "${spring.cloud.openfeign.client.config.bank-service.url}",
        fallbackFactory = BankClientFallbackFactory.class, decode404 = true)
public interface BankClient {

    @GetMapping("/api/account/{accountNumber}/balance")
    BigDecimal getAccountBalance(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/payment/tax")
    void handleTax(@RequestBody TaxDto taxDto);

    @PutMapping("/api/account/{accountNumber}/reserve")
    void updateAvailableBalance(@PathVariable("accountNumber") String accountNumber, @RequestParam BigDecimal amount);

    @PutMapping("/api/account/{accountNumber}/update-balance")
    void updateBalance(@PathVariable("accountNumber") String accountNumber, @RequestParam BigDecimal amount);
}
