package rs.raf.bank_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreditDetailedDTO;
import rs.raf.bank_service.domain.dto.CreditShortDTO;
import rs.raf.bank_service.domain.entity.Credit;
import rs.raf.bank_service.service.CreditService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/credits")
public class CreditController {
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<CreditShortDTO>> getCreditsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditService.getCreditsByAccountNumber(accountNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditDetailedDTO> getCreditById(@PathVariable Long id) {
        Optional<CreditDetailedDTO> credit = creditService.getCreditById(id);
        return credit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Credit> createCredit(@RequestBody Credit credit) {
        return ResponseEntity.ok(creditService.createCredit(credit));
    }

    //TODO prebaciti pravljenje da radi sa dto
}
