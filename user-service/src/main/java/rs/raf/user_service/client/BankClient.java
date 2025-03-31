package rs.raf.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.CardRequestDto;

@FeignClient(name = "bank-service", url = "${spring.cloud.openfeign.client.config.bank-service.url}", fallbackFactory = BankClientFallbackFactory.class)
public interface BankClient {

    @PostMapping("/api/payment/confirm-payment/{id}")
    void confirmPayment(@PathVariable("id") Long id);

    @PostMapping("/api/payment/confirm-transfer/{id}")
    void confirmTransfer(@PathVariable("id") Long id);

    @PutMapping("/api/account/{id}/change-limit")
    void changeAccountLimit(@PathVariable("id") Long id);

    @PutMapping("/api/account/1/cards/approve/{id}")
    void approveCardRequest(@PathVariable("id") Long id);

    @PostMapping("/api/payment/reject-payment/{id}")
    void rejectConfirmPayment(@PathVariable("id") Long id);

    @PostMapping("/api/payment/reject-transfer/{id}")
    void rejectConfirmTransfer(@PathVariable("id") Long id);//

    @PutMapping("/api/account/{id}/change-limit/reject")
    void rejectChangeAccountLimit(@PathVariable("id") Long id);//

    @PutMapping("/api/account/1/cards/reject/{id}")
    void rejectApproveCardRequest(@PathVariable("id") Long id);//

}
