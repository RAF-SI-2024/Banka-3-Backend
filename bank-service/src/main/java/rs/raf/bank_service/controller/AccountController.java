package rs.raf.bank_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.*;
import rs.raf.bank_service.exceptions.*;
import rs.raf.bank_service.repository.ChangeLimitRequestRepository;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;
import java.math.BigDecimal;

@Tag(name = "Bank accounts controller", description = "API for managing bank accounts")
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final ChangeLimitRequestRepository changeLimitRequestRepository;
    private AccountService accountService;

    /// Refaktorisano tako da getAccounts bude jedna GET metoda a ne dve jer tako kod ne radi
    /// Ovde proverava da li se request salje kao klijent ili admin/employee
    /// GET endpoint sa opcionalnim filterima i paginacijom/sortiranjem po prezimenu vlasnika
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('CLIENT')")
    @GetMapping
    public ResponseEntity<?> getAccounts(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String auth) {

        try {
            String role = jwtTokenUtil.getUserRoleFromAuthHeader(auth);

            if (role.equals("CLIENT")) {
                Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
                return ResponseEntity.ok(accountService.getMyAccounts(clientId));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("owner.lastName").ascending());
            Page<AccountDto> accounts = accountService.getAccounts(accountNumber, firstName, lastName, pageable);
            return ResponseEntity.ok(accounts);
        } catch (UserNotAClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred.");
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/bank")
    public ResponseEntity<?> getBankAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            Pageable pageable = PageRequest.of(page, size);
            Page<AccountDto> accounts = accountService.getBankAccounts(pageable);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorMessageDto("Unexpected error occurred."));
        }
    }

    @Operation(summary = "Get client accounts with filtering and pagination")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")})
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{clientId}")
    public ResponseEntity<?> getAccountsForClient(
            @RequestParam(required = false) String accountNumber,
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AccountDto> accounts = accountService.getAccountsForClient(accountNumber, clientId, pageable);
            return ResponseEntity.ok(accounts);
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    @Operation(summary = "Add new bank account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> createBankAccount(@RequestHeader("Authorization") String authorizationHeader,
                                               @RequestBody NewBankAccountDto newBankAccountDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createNewBankAccount(newBankAccountDto, authorizationHeader));
        } catch (ClientNotFoundException | CurrencyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (DuplicateAccountNameException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE')")
    @GetMapping("/details/{accountNumber}")
    @Operation(summary = "Get account details", description = "Returns account details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account with details"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account details retrieval failed")
    })
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String auth,
                                               @PathVariable("accountNumber") String accountNumber) {
        try {
            Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
            String role = jwtTokenUtil.getUserRoleFromAuthHeader(auth);
            return ResponseEntity.ok(accountService.getAccountDetails(role, clientId, accountNumber));
        } catch (ClientNotAccountOwnerException e) {
            return ResponseEntity.badRequest().body(new ErrorMessageDto(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(new ErrorMessageDto(e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('CLIENT')")  // Promenjena provera autentifikacije
    @PutMapping("/{accountNumber}/change-name")
    @Operation(summary = "Change account name", description = "Allows a client to change the name of their account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account name updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> changeAccountName(
            @PathVariable String accountNumber,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ChangeAccountNameDto request) {
        try {
            accountService.changeAccountName(accountNumber, request.getNewName(), authHeader);
            return ResponseEntity.ok("Account name updated successfully");
        } catch (DuplicateAccountNameException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // only called by user service when approved
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/change-limit")
    @Operation(summary = "Change account limit", description = "Allows a client to request a change in account limit.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account limit update request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> changeAccountLimit(@PathVariable Long id) {
        accountService.changeAccountLimit(id);
        return ResponseEntity.ok("Account limit updated successfully");
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/change-limit/reject")
    @Operation(summary = "Reject account limit change", description = "Allows user service to reject request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account limit change request rejected successfully."),
            @ApiResponse(responseCode = "404", description = "Request not found.")
    })
    public ResponseEntity<?> rejectAccountLimitChange(@PathVariable Long id) {
        try {
            accountService.rejectAccountLimitChange(id);
            return ResponseEntity.ok("Request rejected successfully.");
        }catch (ChangeLimitReqNotFoundException | RejectNonPendingRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/{accountNumber}/request-change-limit")
    @Operation(summary = "Request change account limit", description = "Saves a limit change request for approval.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Limit change request saved"),
            @ApiResponse(responseCode = "400", description = "Invalid limit value"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<String> requestChangeAccountLimit(
            @PathVariable String accountNumber,
            @RequestBody @Valid ChangeAccountLimitDto request,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            accountService.requestAccountLimitChange(accountNumber, request.getNewLimit(), authHeader);
            return ResponseEntity.ok("Limit change request saved. Awaiting approval.");
        } catch (InvalidLimitException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("VALJDA SE NECE DESITI");
        }
    }


    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Set authorized person for an account", description = "Assigns an authorized person to a company's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authorized person set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input values"),
            @ApiResponse(responseCode = "404", description = "Account or authorized person not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Unauthorized access")
    })
    @PutMapping("/{companyAccountId}/set-authorized-person")
    public ResponseEntity<?> setAuthorizedPerson(
            @PathVariable Long companyAccountId,
            @RequestParam Long authorizedPersonId,
            @RequestHeader("Authorization") String authHeader) {

        Long employeeId = jwtTokenUtil.getUserIdFromAuthHeader(authHeader);

        try {
            accountService.setAuthorizedPerson(companyAccountId, authorizedPersonId, employeeId);
            return ResponseEntity.ok("Authorized person successfully assigned to account.");
        } catch (AccountNotFoundException | AuthorizedPersonNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (InvalidAuthorizedPersonException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // interni endpoint
    @Operation(summary = "Get client account balance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account balance retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> getAccountBalance(@PathVariable String accountNumber) {
        try {
            return ResponseEntity.ok(accountService.getAccountBalance(accountNumber));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // interni endpoint
    @Operation(summary = "Update account available balance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account available balance updated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountNumber}/reserve")
    public ResponseEntity<?> updateAvailableBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        try {
            accountService.updateAvailableBalance(accountNumber, amount);
            return ResponseEntity.ok("Account available balance updated successfully");
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // interni endpoint
    @Operation(summary = "Update account balance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account balance updated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountNumber}/update-balance")
    public ResponseEntity<?> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        try {
            accountService.updateBalance(accountNumber, amount);
            return ResponseEntity.ok("Account balance updated successfully");
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
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

    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('CLIENT')")
    @GetMapping("/for-order")
    public ResponseEntity<?> getAccountsForOrder(@RequestHeader("Authorization") String auth) {
        try {
            String role = jwtTokenUtil.getUserRoleFromAuthHeader(auth);
            Long userId = jwtTokenUtil.getUserIdFromAuthHeader(auth);

            if (role.equals("CLIENT")) {
                return ResponseEntity.ok(accountService.getMyAccounts(userId));
            } else {
                return ResponseEntity.ok(accountService.getAllBankAccounts());
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error occurred: " + e.getMessage());
        }
    }
}
