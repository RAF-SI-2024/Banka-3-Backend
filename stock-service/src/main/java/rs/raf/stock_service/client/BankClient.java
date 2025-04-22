package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.CreatePaymentDto;
import rs.raf.stock_service.domain.dto.ExecutePaymentDto;
import rs.raf.stock_service.domain.dto.PaymentDto;
import rs.raf.stock_service.domain.dto.TaxDto;
import rs.raf.stock_service.domain.dto.AccountDetailsDto;
import rs.raf.stock_service.domain.dto.ConvertDto;

import java.math.BigDecimal;

/// Klasa koja sluzi za slanje HTTP poziva na bankService
@FeignClient(name = "bank-service", url = "${spring.cloud.openfeign.client.config.bank-service.url}",
        fallbackFactory = BankClientFallbackFactory.class, decode404 = true)
public interface BankClient {

    @GetMapping("/api/account/{accountNumber}/balance")
    BigDecimal getAccountBalance(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/payment/tax")
    void handleTax(@RequestBody ExecutePaymentDto executePaymentDto);
  
    @PostMapping("/api/payment")
    ResponseEntity<PaymentDto> createPayment(@RequestBody CreatePaymentDto dto);

    @GetMapping("/api/account/client/{clientId}/usd-account-number")
    ResponseEntity<String> getUSDAccountNumberByClientId(@PathVariable("clientId") Long clientId);

    @PostMapping("/api/payment/reject-payment/{paymentId}")
    void rejectPayment(@PathVariable("paymentId") Long paymentId);

    @PutMapping("/api/payment/confirm/{paymentId}")
    void confirmPayment(@PathVariable("paymentId") Long paymentId);

    @PostMapping("/api/payment/execute-system-payment")
    ResponseEntity<PaymentDto> executeSystemPayment(@RequestBody ExecutePaymentDto dto);

    @PostMapping("api/exchange-rates/convert")
    BigDecimal convert(@RequestBody ConvertDto convertDto);

    @GetMapping("api/account/details/{accountNumber}")
    AccountDetailsDto getAccountDetails(@PathVariable("accountNumber") String accountNumber);
}
