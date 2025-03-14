package rs.raf.bank_service.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.client.UserClient;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.domain.dto.ChangeAccountNameDto;
import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.service.AccountService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;

@Tag(name = "Bank accounts controller", description = "API for managing bank accounts")
@RestController
@RequestMapping("/api/account")
@AllArgsConstructor
public class AccountController {

    private AccountService accountService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get all accounts with filtering and pagination")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AccountDto>> getAccounts(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("owner.lastName").ascending());
        Page<AccountDto> accounts = accountService.getAccounts(accountNumber, firstName, lastName, pageable);
        return ResponseEntity.ok(accounts);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @Operation(summary = "Get client accounts with filtering and pagination")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")})
    @GetMapping("/{clientId}")
    public ResponseEntity<Page<AccountDto>> getAccountsForClient(
            @RequestParam(required = false) String accountNumber,
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AccountDto> accounts = accountService.getAccountsForClient(accountNumber, clientId, pageable);
        return ResponseEntity.ok(accounts);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
    @PostMapping
    @Operation(summary = "Add new bank account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<Void> createBankAccount(@RequestHeader("Authorization") String authorizationHeader,
                                                  @RequestBody NewBankAccountDto newBankAccountDto) {
        accountService.createNewBankAccount(newBankAccountDto, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping
    @Operation(summary = "Get all client's accounts", description = "Returns a list of all client's accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account list")
    })
    public ResponseEntity<?> getMyAccounts(@RequestHeader("Authorization") String auth) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        return ResponseEntity.ok(accountService.getMyAccounts(clientId));
    }

    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/details/{accountNumber}")
    @Operation(summary = "Get account details", description = "Returns account details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account with details")
    })
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String auth,
                                               @PathVariable("accountNumber") String accountNumber) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        return ResponseEntity.ok(accountService.getAccountDetails(clientId, accountNumber));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/{accountNumber}/change-name")
    @Operation(summary = "Change account name", description = "Allows a client to change the name of their account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account name updated successfully")
    })
    public ResponseEntity<String> changeAccountName(@PathVariable String accountNumber,
                                                    @RequestHeader("Authorization") String authHeader,
                                                    @RequestBody @Valid ChangeAccountNameDto request) {
        accountService.changeAccountName(accountNumber, request.getNewName(), authHeader);
        return ResponseEntity.ok("Account name updated successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/change-limit")
    @Operation(summary = "Change account limit", description = "Allows a client to request a change in account limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account limit update request created successfully")
    })
    public ResponseEntity<String> changeAccountLimit(@PathVariable Long id) {
        accountService.changeAccountLimit(id);
        return ResponseEntity.ok("Account limit updated successfully");
    }

    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/{accountNumber}/request-change-limit")
    @Operation(summary = "Request change account limit", description = "Saves a limit change request for approval.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limit change request saved")
    })
    public ResponseEntity<String> requestChangeAccountLimit(@PathVariable String accountNumber,
                                                            @RequestBody @Valid ChangeAccountLimitDto request,
                                                            @RequestHeader("Authorization") String authHeader) {
        try {
            accountService.requestAccountLimitChange(accountNumber, request.getNewLimit(), authHeader);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        return ResponseEntity.ok("Limit change request saved. Awaiting approval.");
    }
}
