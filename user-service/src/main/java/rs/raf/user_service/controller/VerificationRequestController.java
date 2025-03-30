package rs.raf.user_service.controller;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.ClientDto;
import rs.raf.user_service.domain.dto.CreateVerificationRequestDto;
import rs.raf.user_service.domain.dto.ErrorMessageDto;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.exceptions.RejectNonPendingRequestException;
import rs.raf.user_service.exceptions.VerificationClientNotFoundException;
import rs.raf.user_service.exceptions.VerificationNotFoundException;
import rs.raf.user_service.service.ClientService;
import rs.raf.user_service.service.VerificationRequestService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/verification")
@AllArgsConstructor
public class VerificationRequestController {

    private final VerificationRequestService verificationRequestService;
    private final ClientService clientService;

    @Operation(summary = "Get active verification requests", description = "Returns a list of pending verification requests for the user.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of active requests retrieved successfully"), @ApiResponse(responseCode = "401", description = "Unauthorized access")})
    @GetMapping("/active-requests")
    public ResponseEntity<List<VerificationRequest>> getActiveRequests(
            @RequestHeader(value="User-Agent", required=false) String userAgent
    ) {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ClientDto client = clientService.getCurrentClient();

            List<VerificationRequest> requests = verificationRequestService.getActiveRequests(client.getId());
            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            throw new VerificationClientNotFoundException();
        }

    }

    @Operation(summary = "Get verification request history", description = "Returns a list of non pending verification requests for the user.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of non pending requests retrieved successfully"), @ApiResponse(responseCode = "401", description = "Unauthorized access")})
    @GetMapping("/history")
    public ResponseEntity<List<VerificationRequest>> getRequestHistory(
            @RequestHeader(value="User-Agent", required=false) String userAgent
    ) {
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ClientDto client = clientService.getCurrentClient();

            List<VerificationRequest> requests = verificationRequestService.getRequestHistory(client.getId());
            return ResponseEntity.ok(requests);

        } catch (Exception e) {
            throw new VerificationClientNotFoundException();
        }

    }

    @Operation(summary = "Deny verification request", description = "Denies a verification request for a specific transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification request denied"),
            @ApiResponse(responseCode = "400", description = "Request not found or already processed")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/deny/{requestId}")
    public ResponseEntity<?> denyRequest(
            @RequestHeader String userAgent,
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {
        if (verificationRequestService.calledFromMobile(userAgent))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            verificationRequestService.denyVerificationRequest(requestId, authHeader);
            return ResponseEntity.ok("Request denied successfully.");
        }catch (VerificationNotFoundException | RejectNonPendingRequestException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ovo nije za mobilnu, zove se iz servisa na beku
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/request")
    public ResponseEntity<String> createVerificationRequest(
            @RequestBody @Valid CreateVerificationRequestDto createVerificationRequestDto
    ) {
        verificationRequestService.createVerificationRequest(createVerificationRequestDto);
        return ResponseEntity.ok("Verification request created.");
    }


    @Operation(summary = "Approve verification request", description = "Approves a verification request for a specific transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification request approved"),
            @ApiResponse(responseCode = "400", description = "Request not found or already processed"),
            @ApiResponse(responseCode = "403", description = "Unauthorized access")
    })
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<?> approveRequest(
            @RequestHeader(value="User-Agent", required=false) String userAgent,
            @PathVariable Long requestId,
            @RequestHeader("Authorization") String authHeader) {  // ✅ Dodajemo JWT header
        if (!verificationRequestService.calledFromMobile(userAgent)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean success = verificationRequestService.processApproval(requestId, authHeader);

            return success
                    ? ResponseEntity.ok("Request approved.")
                    : ResponseEntity.badRequest().body(new ErrorMessageDto("Request not found or already processed"));
        } catch (FeignException.BadRequest e) {
            return ResponseEntity.badRequest().body(new ErrorMessageDto(e.getMessage()));
        }


    }

}
