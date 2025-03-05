package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.domain.dto.ChangeAccountNameDto;
import rs.raf.bank_service.domain.dto.ClientDto;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.service.AccountService;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@RestControllerAdvice
public class AccController {

    private final AccountService accountService;
    private final UserClient userClient;

    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('client')")
    @PutMapping("/{id}/change-name")
    @Operation(summary = "Change account name", description = "Allows a client to change the name of their account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account name updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> changeAccountName(
            @PathVariable Long id,
            @RequestBody @Valid ChangeAccountNameDto request) {
        try {
            accountService.changeAccountName(id, request.getNewName());
            return ResponseEntity.ok("Account name updated successfully");
        } catch (DuplicateAccountNameException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('client')")
    @PutMapping("/{id}/change-limit")
    @Operation(summary = "Change account limit", description = "Allows a client to request a change in account limit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account limit update request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> changeAccountLimit(
            @PathVariable Long id,
            @RequestBody @Valid ChangeAccountLimitDto request) {
        try {
            accountService.changeAccountLimit(id, request.getNewLimit(), request.getVerificationCode());
            return ResponseEntity.ok("Account limit updated successfully");
        } catch (InvalidLimitException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @PreAuthorize("hasAuthority('CLIENT') or hasAuthority('client')")
    @PutMapping("/{id}/request-change-limit")
    @Operation(summary = "Request change account limit", description = "Initiates a 2FA verification for changing the account limit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value")
    })
    public ResponseEntity<String> requestChangeAccountLimit(
            @PathVariable Long id,
            @RequestBody @Valid ChangeAccountLimitDto request) {

        ClientDto client = userClient.getClientById(id); // Proveravamo korisnika
        accountService.requestAccountLimitChange(id, client.getEmail(), request.getNewLimit());

        return ResponseEntity.ok("Verification request created. Please verify.");
    }




    // Globalni Exception Handleri
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> handleAccountNotFoundException(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateAccountNameException.class)
    public ResponseEntity<String> handleDuplicateAccountNameException(DuplicateAccountNameException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }


    @ExceptionHandler(AccNotFoundException.class)
    public ResponseEntity<String> handleAccNotFoundException(AccNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }




}

