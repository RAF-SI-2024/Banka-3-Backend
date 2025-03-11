package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.exceptions.*;
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

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Get all loans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loans retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<LoanShortDto>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
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

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Operation(summary = "Approve a loan", description = "Marks the loan as approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveLoan(@PathVariable Long id) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(loanService.approveLoan(id));
        }catch (LoanRequestNotFoundException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
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