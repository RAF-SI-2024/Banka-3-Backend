package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.service.LoanService;

import java.util.List;
import java.util.Optional;

@Tag(name = "Loan Controller", description = "API for managing loans")
@RestController
@RequestMapping("/loans")
public class LoanController {
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

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

    @Operation(summary = "Create a new loan")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/create")
    public ResponseEntity<LoanDto> createLoan(@RequestBody LoanDto loanDto) {
        return ResponseEntity.ok(loanService.saveLoan(loanDto));
    }
}