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
import rs.raf.bank_service.domain.dto.LoanRequestDto;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.service.LoanRequestService;

import javax.validation.Valid;

@Tag(name = "Loan Requests Controller", description = "API for managing loan requests")
@RestController
@AllArgsConstructor
@RequestMapping("/api/loan-requests")
public class LoanRequestController {
    private final LoanRequestService loanRequestService;

    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get all loan requests for client")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loan requests retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<Page<LoanRequestDto>> getClientLoanRequests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(loanRequestService.getClientLoanRequests(authHeader, PageRequest.of(page, size)));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Create a new loan request")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<?> createLoanRequest(@RequestBody @Valid CreateLoanRequestDto createLoanRequestDto, @RequestHeader("Authorization") String authHeader) {
            loanRequestService.saveLoanRequest(createLoanRequestDto, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(createLoanRequestDto);
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Operation(summary = "Approve a loan", description = "Marks the loan as approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan approved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveLoan(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(loanRequestService.approveLoan(id));
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Operation(summary = "Reject a loan", description = "Marks the loan as rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @PutMapping("/reject/{id}")
    public ResponseEntity<LoanRequestDto> rejectLoan(@PathVariable Long id) {
        LoanRequestDto rejectedLoan = loanRequestService.rejectLoan(id);
        return ResponseEntity.ok(rejectedLoan);
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @Operation(summary = "Get all loan requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of loan requests retrieved successfully")
    })
    @GetMapping("/all")
    public ResponseEntity<Page<LoanRequestDto>> getAllLoanRequests(
            @RequestParam(required = false) LoanType type,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(loanRequestService.getAllLoanRequests(type, accountNumber, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }


}
