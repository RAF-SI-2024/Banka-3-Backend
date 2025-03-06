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
import rs.raf.bank_service.domain.entity.ChangeLimitRequest;
import rs.raf.bank_service.domain.enums.VerificationStatus;
import rs.raf.bank_service.exceptions.AccNotFoundException;
import rs.raf.bank_service.exceptions.AccountNotFoundException;
import rs.raf.bank_service.exceptions.DuplicateAccountNameException;
import rs.raf.bank_service.exceptions.InvalidLimitException;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
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
    private final ChangeLimitRequestRepository changeLimitRequestRepository;

    @PreAuthorize("isAuthenticated()")  // Promenjena provera autentifikacije
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

    @PreAuthorize("isAuthenticated()")  // Promenjena provera autentifikacije
    @PutMapping("/{id}/change-limit")
    @Operation(summary = "Change account limit", description = "Allows a client to request a change in account limit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account limit update request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> changeAccountLimit(@PathVariable Long id, @RequestBody @Valid ChangeAccountLimitDto request) {
        accountService.changeAccountLimit(id); // Ova linija može baciti exception, koji se obrađuje globalno
        return ResponseEntity.ok("Account limit updated successfully");
    }

    @PreAuthorize("isAuthenticated()")  // Promenjena provera autentifikacije
    @PutMapping("/{id}/request-change-limit")
    @Operation(summary = "Request change account limit", description = "Saves a limit change request for approval.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Limit change request saved"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<String> requestChangeAccountLimit(
            @PathVariable Long id,
            @RequestBody @Valid ChangeAccountLimitDto request) {
        try {
            if (request.getNewLimit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidLimitException();
            }

            // Čuvamo zahtev u bazi sa statusom PENDING
            ChangeLimitRequest limitRequest = new ChangeLimitRequest(id, request.getNewLimit());
            changeLimitRequestRepository.save(limitRequest);

            return ResponseEntity.ok("Limit change request saved. Awaiting approval.");
        } catch (InvalidLimitException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }





    // Globalni Exception Handleri

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }


    @ExceptionHandler(InvalidLimitException.class)
    public ResponseEntity<String> handleInvalidLimitException(InvalidLimitException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> handleAccountNotFoundException(AccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(AccNotFoundException.class)
    public ResponseEntity<String> handleAccNotFoundException(AccNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }


    @ExceptionHandler(DuplicateAccountNameException.class)
    public ResponseEntity<String> handleDuplicateAccountNameException(DuplicateAccountNameException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }



}

