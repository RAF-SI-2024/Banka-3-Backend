package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.bank.BankClient;
import rs.raf.user_service.dto.ClientDto;
import rs.raf.user_service.dto.PaymentVerificationRequestDto;
import rs.raf.user_service.dto.RequestConfirmedDto;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.enums.VerificationType;
import org.springframework.security.access.prepost.PreAuthorize;
import rs.raf.user_service.dto.VerificationRequestDto;
import rs.raf.user_service.exceptions.VerificationClientNotFoundException;
import rs.raf.user_service.service.ClientService;
import rs.raf.user_service.service.VerificationRequestService;
import java.util.List;

@RestController
@RequestMapping("/api/verification")
public class VerificationRequestController {

    private final VerificationRequestService verificationRequestService;
    private final ClientService clientService;
    private final BankClient bankClient;

    public VerificationRequestController(VerificationRequestService verificationRequestService, ClientService clientService, BankClient bankClient) {
        this.verificationRequestService = verificationRequestService;
        this.clientService = clientService;
        this.bankClient = bankClient;
    }

    @Operation(summary = "Get active verification requests", description = "Returns a list of pending verification requests for the user.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of active requests retrieved successfully"), @ApiResponse(responseCode = "401", description = "Unauthorized access")})
    @GetMapping("/active-requests")
    public ResponseEntity<List<VerificationRequest>> getActiveRequests() {

        try {
            ClientDto client = clientService.getCurrentClient();

            List<VerificationRequest> requests = verificationRequestService.getActiveRequests(client.getId());
            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            throw new VerificationClientNotFoundException();
        }

    }

    @Operation(summary = "Approve verification request", description = "Approves a verification request for a specific transaction.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Verification request approved"), @ApiResponse(responseCode = "400", description = "Request not found or already processed")})
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<String> approveRequest(@PathVariable Long requestId) {
        boolean updated = verificationRequestService.updateRequestStatus(requestId, VerificationStatus.APPROVED);
        VerificationType verificationType = verificationRequestService.getVerificationTypeByRequestId(requestId);

        if (updated) {
            // Poziv servisne metode koja uzima requestId, iz baze uzima transactionId i prosleđuje ga ka banki
            Long transactionId = verificationRequestService.getTransactionIdByRequestId(requestId);
            // Slanje transactionId bank servisu za izvršenje transfera

            if (verificationType == VerificationType.TRANSFER) {
                bankClient.confirmTransfer(new RequestConfirmedDto(transactionId));
            } else if (verificationType == VerificationType.PAYMENT) {
                bankClient.confirmPayment(new RequestConfirmedDto(transactionId));
            }
        }
        return updated ? ResponseEntity.ok("Request approved") : ResponseEntity.badRequest().body("Request not found or already processed");
    }

    @Operation(summary = "Deny verification request", description = "Denies a verification request for a specific transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification request denied"),
            @ApiResponse(responseCode = "400", description = "Request not found or already processed")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/deny/{requestId}")
    public ResponseEntity<String> denyRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {

        boolean success = verificationRequestService.denyVerificationRequest(requestId, authHeader);

        return success
                ? ResponseEntity.ok("Request denied")
                : ResponseEntity.badRequest().body("Request not found or already processed");
    }

    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('admin')")
    @PostMapping("/request")
    public ResponseEntity<String> createVerificationRequest(@RequestBody VerificationRequestDto request) {
        verificationRequestService.createVerificationRequest(
                request.getUserId(),
                request.getEmail(),
                request.getTargetId()
        );
        return ResponseEntity.ok("Verification request created.");
    }

    @Operation(summary = "Approve verification request", description = "Approves a verification request for a specific transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification request approved"),
            @ApiResponse(responseCode = "400", description = "Request not found or already processed"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<String> approveRequest(
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {  // ✅ Dodajemo JWT header

        boolean success = verificationRequestService.processApproval(requestId, authHeader);

        return success
                ? ResponseEntity.ok("Request approved and account limit updated")
                : ResponseEntity.badRequest().body("Request not found or already processed");
    }

}
