package rs.raf.stock_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.CreatePaymentDto;
import rs.raf.stock_service.domain.dto.ExecutePaymentDto;
import rs.raf.stock_service.domain.dto.PaymentDto;
import rs.raf.stock_service.domain.dto.TaxDto;

import java.math.BigDecimal;


/// Klasa koja sluzi za slanje HTTP poziva na bankService
@FeignClient(name = "bank-service", url = "${spring.cloud.openfeign.client.config.bank-service.url}", decode404 = true)
public interface BankClient {

    @GetMapping("/api/account/{accountNumber}/balance")
    BigDecimal getAccountBalance(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/payment/tax")
    void handleTax(@RequestBody TaxDto taxDto);

    @PostMapping("/api/payment")
    ResponseEntity<PaymentDto> createPayment(@RequestBody CreatePaymentDto dto);

    @GetMapping("/api/account/client/{clientId}/account-number")
    ResponseEntity<String> getAccountNumberByClientId(@PathVariable("clientId") Long clientId);

    @PostMapping("/api/payment/reject-payment/{paymentId}")
    void rejectPayment(@PathVariable("paymentId") Long paymentId);

    @PutMapping("/api/payment/confirm/{paymentId}")
    void confirmPayment(@PathVariable("paymentId") Long paymentId);

    @PostMapping("/api/payment/execute-system-payment")
    ResponseEntity<PaymentDto> executeSystemPayment(@RequestBody ExecutePaymentDto dto);




}
