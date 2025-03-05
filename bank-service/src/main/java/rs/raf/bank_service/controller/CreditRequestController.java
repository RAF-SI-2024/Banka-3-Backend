package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreditRequestDto;
import rs.raf.bank_service.domain.entity.CreditRequest;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;
import rs.raf.bank_service.service.CreditRequestService;

import java.util.List;

@Tag(name = "Credit request controller", description = "API for managing credit requests")
@RestController
@RequestMapping("/credit-requests")
public class CreditRequestController {
    private final CreditRequestService creditRequestService;

    public CreditRequestController(CreditRequestService creditRequestService) {
        this.creditRequestService = creditRequestService;
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Submit a new credit request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<CreditRequest> submitRequest(@RequestBody CreditRequest creditRequest) {
        return ResponseEntity.ok(creditRequestService.submitCreditRequest(creditRequest));
    }

    @PreAuthorize("hasAuthority('employee')")
    @GetMapping("/status/{approval}")
    @Operation(summary = "Get credit requests by approval status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully fetched credit requests"),
            @ApiResponse(responseCode = "404", description = "No requests found for the specified status")
    })
    public ResponseEntity<List<CreditRequestDto>> getPendingRequests(@PathVariable CreditRequestApproval approval) {
        return ResponseEntity.ok(creditRequestService.getRequestsByStatus(approval));
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a credit request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit request accepted successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found")
    })
    public ResponseEntity<CreditRequest> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.acceptRequest(id));
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping("/{id}/deny")
    @Operation(summary = "Deny a credit request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit request denied successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found")
    })
    public ResponseEntity<CreditRequest> denyRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.denyRequest(id));
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping("/{id}/pending")
    @Operation(summary = "Set a credit request as pending")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit request set to pending successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found")
    })
    public ResponseEntity<CreditRequest> pendingRequest(@PathVariable Long id) {
        return ResponseEntity.ok(creditRequestService.pendingRequest(id));
    }
}
