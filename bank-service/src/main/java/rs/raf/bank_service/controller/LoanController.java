package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.service.LoanService;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;

import java.util.List;
import java.util.Optional;


@Tag(name = "Loan Controller", description = "API for managing loans")
@RestController
@RequestMapping("/api/loan")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }


    @PostMapping("/approve/{loanRequestId}")
    @Operation(summary = "Approve a loan request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan request ID or request already processed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    public ResponseEntity<Loan> approveLoan(@PathVariable Long loanRequestId) {
        return ResponseEntity.ok(loanService.approveLoan(loanRequestId));
    }

    @PostMapping("/reject/{loanRequestId}")
    @Operation(summary = "Reject a loan request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan request rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan request ID or request already processed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access")
    })
    public ResponseEntity<String> rejectLoan(@PathVariable Long loanRequestId) {
        loanService.rejectLoan(loanRequestId);
        return ResponseEntity.ok("Loan request rejected.");

    @Operation(summary = "Get all loans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loans retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<LoanShortDto>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @Operation(summary = "Get loan by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        Optional<LoanDto> loan = loanService.getLoanById(id);
        return loan.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    }
}