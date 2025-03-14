package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/payment")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenUtil jwtTokenUtil;

    @PreAuthorize("hasRole('CLIENT') or hasRole('ADMIN')")
    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts", description = "Transfers funds from one account to another. Both must be using the same currency.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer created successfully, waiting for confirmation.")
    })
    public ResponseEntity<String> createTransfer(@Valid @RequestBody TransferDto dto,
                                                 @RequestHeader("Authorization") String token) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        try {
            boolean success = paymentService.createTransferPendingConfirmation(dto, clientId);
            if (success) {
                return ResponseEntity.ok("Transfer created successfully, waiting for confirmation.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Transfer creation failed: Insufficient funds or invalid data");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/confirm-transfer/{paymentId}")
    @Operation(summary = "Confirm and execute transfer", description = "Confirm transfer and execute funds transfer between accounts after verification.")
    public ResponseEntity<String> confirmTransfer(@PathVariable Long paymentId) {
        boolean success = paymentService.confirmTransferAndExecute(paymentId);
        if (success) {
            return ResponseEntity.ok("Transfer completed successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer confirmation failed.");
        }
    }
}
