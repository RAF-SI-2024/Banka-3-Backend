package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreatePaymentDto;
import rs.raf.bank_service.domain.dto.PaymentDetailsDto;
import rs.raf.bank_service.domain.dto.PaymentOverviewDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payment")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtTokenUtil jwtTokenUtil;

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts", description = "Transfers funds from one account to another. Both must " +
            "be using the same currency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data, not same currency or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Validation or transfer creation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> createTransfer(
            @Valid @RequestBody TransferDto dto,
            @RequestHeader("Authorization") String token) {

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);

        try {
            boolean success = paymentService.createTransferPendingConfirmation(dto, clientId);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK).body("Transfer created successfully, waiting for confirmation.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer creation failed: Insufficient funds or invalid data");
            }
        } catch (SenderAccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sender account not found: " + e.getMessage());
        } catch (ReceiverAccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Receiver account not found: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient funds: " + e.getMessage());
        } catch (NotSameCurrencyForTransferException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Currency mismatch: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @PostMapping("/confirm-transfer/{paymentId}")
    @Operation(summary = "Confirm and execute transfer", description = "Confirm transfer and execute funds transfer between accounts after verification.")
    public ResponseEntity<String> confirmTransfer(@PathVariable Long paymentId) {
        try {
            boolean success = paymentService.confirmTransferAndExecute(paymentId);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK).body("Transfer completed successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer failed.");
            }
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found: " + e.getMessage());
        } catch (UnauthorizedTransferConormationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to complete transfer: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('CLIENT') or hasRole('ADMIN')")
    //Metoda za zapocinjanje placanja, al ne izvrsava je sve dok se ne odradi verifikacija pa se odradjuje druga metoda.
    @PostMapping()
    @Operation(summary = "Make a payment", description = "Executes a payment from the sender's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "Payment creation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> newPayment(
            @Valid @RequestBody CreatePaymentDto dto,
            @RequestHeader("Authorization") String token) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        try {
            boolean success = paymentService.createPaymentBeforeConfirmation(dto, clientId);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK).body("Payment created successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed: Insufficient funds or invalid data");
            }
        } catch (InsufficientFundsException | PaymentCodeNotProvidedException | PurposeOfPaymentNotProvidedException | SenderAccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            // @todo FIXATI ERROR HANDLING SVUDA ROKNUCU SE
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Metoda za potvrdu plaćanja
    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping("/confirm-payment/{paymentId}")
    @Operation(summary = "Confirm payment", description = "Confirm and execute payment once the receiver is verified.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment completed successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "400", description = "Payment validation error"),
            @ApiResponse(responseCode = "500", description = "Internal server error while processing payment")
    })
    public ResponseEntity<String> confirmPayment(@PathVariable Long paymentId) {
        try {
            paymentService.confirmPayment(paymentId);
            return ResponseEntity.status(HttpStatus.OK).body("Payment completed successfully.");
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found: " + e.getMessage());
        } catch (UnauthorizedPaymentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to complete payment: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('CLIENT') or hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get payments page filtered", description = "Get filtered page of payments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments returned successfully"),
    })
    public ResponseEntity<Page<PaymentOverviewDto>> getPayments(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String cardNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentOverviewDto> payments = paymentService.getPayments(token, startDate, endDate, minAmount, maxAmount, paymentStatus, accountNumber, cardNumber, pageable);
        return ResponseEntity.ok(payments);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    @Operation(summary = "Get payment details", description = "Get payment details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments returned successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
    })
    public ResponseEntity<PaymentDetailsDto> getPaymentDetails(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        try {
            PaymentDetailsDto details = paymentService.getPaymentDetails(token, id);
            return ResponseEntity.ok(details);
        }
        catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }
}