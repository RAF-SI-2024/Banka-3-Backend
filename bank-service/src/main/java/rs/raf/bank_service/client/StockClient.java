package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.configuration.InternalClientConfig;
import rs.raf.bank_service.domain.dto.*;

import javax.validation.Valid;
import java.util.List;


/// Klasa koja sluzi za slanje HTTP poziva na userService
@FeignClient(name = "stock-service", url = "${spring.cloud.openfeign.client.config.stock-service.url}", fallbackFactory = UserClientFallbackFactory.class, decode404 = true, configuration = InternalClientConfig.class)
public interface StockClient {
    @PostMapping("/api/tracked-payment/success")
    void notifySuccess(TrackedPaymentNotifyDto trackedPaymentNotifyDto);

    @PostMapping("/api/tracked-payment/fail")
    void notifyFail(TrackedPaymentNotifyDto trackedPaymentNotifyDto);
}


