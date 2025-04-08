package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import rs.raf.stock_service.domain.dto.*;

import java.math.BigDecimal;


/// Klasa koja sluzi za slanje HTTP poziva na bankService
@FeignClient(name = "bank-service", url = "${spring.cloud.openfeign.client.config.bank-service.url}", decode404 = true)
public interface BankClient {

    @GetMapping("/api/account/{accountNumber}/balance")
    BigDecimal getAccountBalance(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/payment/tax")
    void handleTax(@RequestBody TaxDto taxDto);

    @PostMapping("api/exchange-rates/convert")
    BigDecimal convert(@RequestBody ConvertDto convertDto);

    @GetMapping("api/account/details/{accountNumber}")
    AccountDetailsDto getAccountDetails(@PathVariable("accountNumber") String accountNumber);
}
