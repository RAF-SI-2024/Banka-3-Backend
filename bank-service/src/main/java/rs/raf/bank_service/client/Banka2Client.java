package rs.raf.bank_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import rs.raf.bank_service.domain.dto.InterBankTransactionRequest;
import rs.raf.bank_service.domain.dto.InterBankTransactionResponse;

@FeignClient(name = "banka2", url = "${url.banke2}")
public interface Banka2Client {
    @PostMapping("/api/interbank/prepare")
    ResponseEntity<InterBankTransactionResponse> prepare(@RequestBody InterBankTransactionRequest request);

    @PostMapping("/api/interbank/commit")
    ResponseEntity<Void> commit(@RequestBody InterBankTransactionRequest request);

    @PostMapping("/api/interbank/cancel")
    ResponseEntity<Void> cancel(@RequestBody InterBankTransactionRequest request);
}
