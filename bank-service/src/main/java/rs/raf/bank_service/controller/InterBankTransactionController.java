package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.InterBankTransactionRequest;
import rs.raf.bank_service.domain.dto.InterBankTransactionResponse;
import rs.raf.bank_service.service.InterBankTransactionService;

@RestController
@RequestMapping("/api/interbank")
@RequiredArgsConstructor
public class InterBankTransactionController {

    private final InterBankTransactionService service;

    @PostMapping("/prepare")
    public ResponseEntity<InterBankTransactionResponse> prepare(@RequestBody InterBankTransactionRequest request) {
        return ResponseEntity.ok(service.prepare(request));
    }

    @PostMapping("/commit")
    public ResponseEntity<Void> commit(@RequestBody InterBankTransactionRequest request) {
        service.commit(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@RequestBody InterBankTransactionRequest request) {
        service.cancel(request);
        return ResponseEntity.ok().build();
    }
}
