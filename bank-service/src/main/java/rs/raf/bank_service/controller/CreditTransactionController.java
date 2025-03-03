package rs.raf.bank_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.CreditTransactionDTO;
import rs.raf.bank_service.service.CreditTransactionService;

import java.util.List;

@RestController
@RequestMapping("/credit-transactions")
public class CreditTransactionController {
    private final CreditTransactionService creditTransactionService;

    public CreditTransactionController(CreditTransactionService creditTransactionService) {
        this.creditTransactionService = creditTransactionService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<CreditTransactionDTO>> findByCreditID(@PathVariable Long id) {
        return ResponseEntity.ok(creditTransactionService.getTransactionsByCreditId(id));
    }
}
