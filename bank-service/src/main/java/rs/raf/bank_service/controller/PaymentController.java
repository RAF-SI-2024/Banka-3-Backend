package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.domain.entity.Account;
import rs.raf.bank_service.domain.enums.PaymentStatus;
import rs.raf.bank_service.domain.enums.TransactionType;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.AccountRepository;
import rs.raf.bank_service.repository.CompanyAccountRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.service.TransactionQueueService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Slf4j
@RestController
@RequestMapping("/api/payment")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionQueueService transactionQueueService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final CompanyAccountRepository companyAccountRepository;

    @PreAuthorize("hasRole('CLIENT')")
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
    public ResponseEntity<?> createTransfer(
            @Valid @RequestBody TransferDto dto,
            @RequestHeader("Authorization") String token) {

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);

        try {
            transactionQueueService.queueTransaction(TransactionType.CREATE_TRANSFER, dto, clientId);
            return ResponseEntity.status(HttpStatus.OK).body("Transfer creation successful");
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

    //Metoda za zapocinjanje placanja, al ne izvrsava je sve dok se ne odradi verifikacija pa se odradjuje druga metoda.
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping()
    @Operation(summary = "Make a payment", description = "Executes a payment from the sender's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @ApiResponse(responseCode = "404", description = "Bad request."),

    })
    public ResponseEntity<?> newPayment(
            @Valid @RequestBody CreatePaymentDto dto,
            @RequestHeader("Authorization") String token) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(token);
        try {
            transactionQueueService.queueTransaction(TransactionType.CREATE_PAYMENT, dto, clientId);
            return ResponseEntity.status(HttpStatus.OK).body("Payment created successfully.");
        } catch (PaymentCodeNotProvidedException | PurposeOfPaymentNotProvidedException |
                 SenderAccountNotFoundException | ReceiverAccountNotFoundException | InsufficientFundsException e
        ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EXTERNAL_BANK')")
    @PostMapping("/external")
    @Operation(summary = "Make a payment", description = "Executes a payment from the sender's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @ApiResponse(responseCode = "404", description = "Bad request."),

    })
    public ResponseEntity<?> createIncomingExternalPayment(
            @Valid @RequestBody CreatePaymentDto dto) {
        try {
            PaymentDto payment = paymentService.initializeIncomingExternalPayment(dto);
            return ResponseEntity.status(HttpStatus.OK).body(payment);
        } catch (PaymentCodeNotProvidedException | PurposeOfPaymentNotProvidedException | ReceiverAccountNotFoundException | InsufficientFundsException e
        ) {
            log.error("Error initializing external payment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('EXTERNAL_BANK')")
    @PutMapping("/{id}/status")
    @Operation(summary = "Make a payment", description = "Executes a payment from the sender's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @ApiResponse(responseCode = "404", description = "Bad request."),

    })
    public ResponseEntity<?> notifyPaymentStatus(@PathVariable Long id, @RequestBody @Valid NotifyPaymentStatusDto dto) {
        try {
            paymentService.handleExternalPaymentStatusUpdate(id, dto);
            return ResponseEntity.status(HttpStatus.OK).body(true);
        } catch (PaymentCodeNotProvidedException | PurposeOfPaymentNotProvidedException | ReceiverAccountNotFoundException | InsufficientFundsException e
        ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/tax")
    public ResponseEntity<?> handleTax(
            @RequestBody ExecutePaymentDto executePaymentDto) {
        try {
            Account stateAccount = companyAccountRepository.findByCompanyId(2L);
            executePaymentDto.getCreatePaymentDto().setReceiverAccountNumber(stateAccount.getAccountNumber());
            transactionQueueService.queueTransaction(TransactionType.SYSTEM_PAYMENT, executePaymentDto.getCreatePaymentDto(), executePaymentDto.getClientId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // Metoda za potvrdu plaćanja
    @PreAuthorize("hasRole('ADMIN')")
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
            transactionQueueService.queueTransaction(TransactionType.CONFIRM_PAYMENT, paymentId);
            return ResponseEntity.status(HttpStatus.OK).body("Payment completed successfully.");
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found: " + e.getMessage());
        } catch (UnauthorizedPaymentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized access: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to complete payment: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject-payment/{paymentId}")
    @Operation(summary = "Reject payment", description = "Rejects a payment")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found, or rejecting a non pending request."),
    })
    public ResponseEntity<String> rejectPayment(@PathVariable Long paymentId) {
        try {
            transactionQueueService.queueTransaction(TransactionType.REJECT_PAYMENT, paymentId);
            return ResponseEntity.status(HttpStatus.OK).body("Payment rejected successfully.");
        } catch (PaymentNotFoundException | RejectNonPendingRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE')")
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

    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE')")
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
        } catch (PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    //cto
    @PostMapping("/execute-system-payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> executeSystemPayment(@RequestBody ExecutePaymentDto dto) {
        try {
            transactionQueueService.queueTransaction(TransactionType.SYSTEM_PAYMENT, dto.getCreatePaymentDto(), dto.getClientId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    /// ExceptionHandlers
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<String> handlePaymentNotFoundException(PaymentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(UnauthorizedPaymentException.class)
    public ResponseEntity<String> handleUnauthorizedPaymentException(UnauthorizedPaymentException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("Greška u PaymentController-u:", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred.");
    }
}