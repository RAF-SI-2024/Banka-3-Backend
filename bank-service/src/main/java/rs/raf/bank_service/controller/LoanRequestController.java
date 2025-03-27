package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreateLoanRequestDto;
import rs.raf.bank_service.domain.dto.LoanDto;
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.exceptions.InvalidLoanTypeException;
import rs.raf.bank_service.exceptions.LoanRequestNotFoundException;
import rs.raf.bank_service.exceptions.UnauthorizedException;
import rs.raf.bank_service.service.LoanRequestService;
import rs.raf.bank_service.service.TransactionQueueService;

import javax.validation.Valid;

@Tag(name = "Loan Requests Controller", description = "API for managing loan requests")
@RestController
@AllArgsConstructor
@RequestMapping("/api/loan-requests")
public class LoanRequestController {
    private final LoanRequestService loanRequestService;
    private final TransactionQueueService transactionQueueService;

    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get all loan requests for client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loan requests retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<?> getClientLoanRequests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<LoanRequestDto> loanRequests = loanRequestService.getClientLoanRequests(authHeader, PageRequest.of(page, size));
            return ResponseEntity.ok(loanRequests);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Create a new loan request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody @Valid CreateLoanRequestDto createLoanRequestDto, @RequestHeader("Authorization") String authHeader) {
        try {
            loanRequestService.saveLoanRequest(createLoanRequestDto, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body("Loan request created successfully.");
        } catch (InvalidLoanTypeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Approve a loan", description = "Marks the loan as approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveLoan(@PathVariable Long id) {
        try {
            LoanDto approvedLoan = transactionQueueService.queueLoan("APPROVE_LOAN", id);
            return ResponseEntity.ok(approvedLoan);
        } catch (LoanRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Reject a loan", description = "Marks the loan as rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/reject/{id}")
    public ResponseEntity<LoanRequestDto> rejectLoan(@PathVariable Long id) {
        try {
            LoanRequestDto rejectedLoan = loanRequestService.rejectLoan(id);
            return ResponseEntity.ok(rejectedLoan);
        } catch (LoanRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all loan requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loan requests retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    @GetMapping("/all")
    public ResponseEntity<Page<LoanRequestDto>> getAllLoanRequests(
            @RequestParam(required = false) LoanType type,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<LoanRequestDto> loanRequests = loanRequestService.getAllLoanRequests(
                    type, accountNumber, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
            return ResponseEntity.ok(loanRequests);
        } catch (InvalidLoanTypeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /// ExceptionHandlers
    @ExceptionHandler(LoanRequestNotFoundException.class)
    public ResponseEntity<String> handleLoanRequestNotFoundException(LoanRequestNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
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
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred.");
    }
}
