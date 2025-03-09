package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.service.LoanService;




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


    }
}