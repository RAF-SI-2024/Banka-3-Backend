package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.service.LoanService;

import java.util.List;
import java.util.Optional;

@Tag(name = "Loan Controller", description = "API for managing loans")
@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @Operation(summary = "Get all loans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loans retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<LoanShortDto>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
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

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @Operation(summary = "Create a new loan")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/create")
    public ResponseEntity<LoanDto> createLoan(@RequestBody LoanDto loanDto) {
        return ResponseEntity.ok(loanService.saveLoan(loanDto));
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @Operation(summary = "Approve a loan", description = "Marks the loan as approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<LoanDto> approveLoan(@PathVariable Long id) {
        LoanDto approvedLoan = loanService.approveLoan(id);
        return ResponseEntity.ok(approvedLoan);
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @Operation(summary = "Reject a loan", description = "Marks the loan as rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/{id}/reject")
    public ResponseEntity<LoanDto> rejectLoan(@PathVariable Long id) {
        LoanDto rejectedLoan = loanService.rejectLoan(id);
        return ResponseEntity.ok(rejectedLoan);
    }
}