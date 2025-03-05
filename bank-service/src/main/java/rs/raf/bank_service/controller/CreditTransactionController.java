package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.CreditTransactionDto;
import rs.raf.bank_service.service.CreditTransactionService;

import java.util.List;

@Tag(name = "Credit transaction controller", description = "API for managing credit transactions")
@RestController
@RequestMapping("/credit-transactions")
public class CreditTransactionController {
    private final CreditTransactionService creditTransactionService;

    public CreditTransactionController(CreditTransactionService creditTransactionService) {
        this.creditTransactionService = creditTransactionService;
    }

    @PreAuthorize("hasAuthority('employee')")
    @GetMapping("/{id}")
    @Operation(summary = "Get all transactions for a specific credit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully fetched credit transactions"),
            @ApiResponse(responseCode = "404", description = "Credit not found")
    })
    public ResponseEntity<List<CreditTransactionDto>> findByCreditID(@PathVariable Long id) {
        return ResponseEntity.ok(creditTransactionService.getTransactionsByCreditId(id));
    }
}
