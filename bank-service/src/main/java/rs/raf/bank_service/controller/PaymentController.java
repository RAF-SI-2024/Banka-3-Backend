package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.PaymentDto;
import rs.raf.bank_service.domain.dto.TransferDto;
import rs.raf.bank_service.service.PaymentService;
import rs.raf.bank_service.exceptions.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts", description = "Transfers funds from one account to another. Both must " +
            "be using the same currency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input data, not same currency or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<String> transferFunds(@RequestBody TransferDto transferDto) {
        /// AUTH - JWT ? , SLANJE NA FON.
        try {
            boolean success = paymentService.transferFunds(transferDto);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK).body("Transfer successful");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer failed: Insufficient funds or invalid data");
            }
        } catch (SenderAccountNotFoundException | ReceiverAccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InsufficientFundsException | NotSameCurrencyForTransferException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @PostMapping("/new")
    @Operation(summary = "Make a payment", description = "Executes a payment from the sender's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient funds"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<String> makePayment(@RequestBody PaymentDto paymentDto) {
        /// AUTH - JWT ? , SLANJE NA FON.
        try {
            boolean success = paymentService.makePayment(paymentDto);
            if (success) {
                return ResponseEntity.status(HttpStatus.OK).body("Payment successful");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment failed: Insufficient funds or invalid data");
            }
        } catch (SenderAccountNotFoundException | ReceiverAccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InsufficientFundsException | PaymentCodeNotProvidedException | PurposeOfPaymentNotProvidedException |
                 SendersAccountsCurencyIsNotDinarException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }
}
