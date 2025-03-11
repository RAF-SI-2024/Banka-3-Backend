package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.LoanRequestStatus;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.service.LoanRequestService;

import java.util.List;

@Tag(name = "Loan Requests Controller", description = "API for managing loan requests")
@RestController
@RequestMapping("/api/loan-requests")
public class LoanRequestController {
    private final LoanRequestService loanRequestService;

    public LoanRequestController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Get loan requests by status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan requests retrieved successfully")
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanRequestDto>> getLoanRequestsByStatus(@PathVariable LoanRequestStatus status) {
        return ResponseEntity.ok(loanRequestService.getLoanRequestsByStatus(status));
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @Operation(summary = "Create a new loan request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody LoanRequestDto loanRequestDto) {
        try {
            loanRequestService.saveLoanRequest(loanRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(loanRequestDto);
        } catch (AccountNotFoundException | CurrencyNotFoundException | InvalidLoanTypeException |
                 InvalidEmploymentStatusException | InvalidInterestRateTypeException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
