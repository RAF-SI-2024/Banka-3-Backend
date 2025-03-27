package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.InstallmentDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanShortDto;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.exceptions.InvalidLoanStatusException;
import rs.raf.bank_service.exceptions.InvalidLoanTypeException;
import rs.raf.bank_service.exceptions.LoanNotFoundException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
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

    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get all loans for client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loans retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<Page<LoanShortDto>> getClientLoans(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<LoanShortDto> loans = loanService.getClientLoans(authHeader,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "amount")));
            return ResponseEntity.ok(loans);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // nema provere autorizacije sry mozda nekad fixati
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get all loan installments for loan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loans retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/{id}/installments")
    public ResponseEntity<List<InstallmentDto>> getLoanInstallments(@PathVariable Long id) {
        try {
            List<InstallmentDto> installments = loanService.getLoanInstallments(id);
            return ResponseEntity.ok(installments);
        } catch (LoanNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // nema provere autorizacije sry mozda nekad fixati
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get loan by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        try {
            Optional<LoanDto> loan = loanService.getLoanById(id);
            return loan.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (LoanNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all loans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/all")
    public ResponseEntity<Page<LoanDto>> getAllLoans(
            @RequestParam(required = false) LoanType type,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("account.accountNumber").ascending());
            Page<LoanDto> loans = loanService.getAllLoans(type, accountNumber, status, pageable);
            return ResponseEntity.ok(loans);
        } catch (InvalidLoanTypeException | InvalidLoanStatusException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /// ExceptionHandlers
    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<String> handleLoanNotFoundException(LoanNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(InvalidLoanStatusException.class)
    public ResponseEntity<String> handleInvalidLoanStatusException(InvalidLoanStatusException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(InvalidLoanTypeException.class)
    public ResponseEntity<String> handleInvalidLoanTypeException(InvalidLoanTypeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred.");
    }
}