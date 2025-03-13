package rs.raf.bank_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

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
import rs.raf.bank_service.domain.entity.ChangeLimitRequest;
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

    private AccountService accountService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserClient userClient;
    private final ChangeLimitRequestRepository changeLimitRequestRepository;



    /// GET endpoint sa opcionalnim filterima i paginacijom/sortiranjem po prezimenu vlasnika
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

    public ResponseEntity<String> createBankAccount(@RequestHeader("Authorization") String authorizationHeader, @RequestBody NewBankAccountDto newBankAccountDto) {
        try {
            accountService.createNewBankAccount(newBankAccountDto, authorizationHeader);
//            if(newBankAccountDto.isCreateCard()){
//                accountService.createCard...
//            }
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (ClientNotFoundException | CurrencyNotFoundException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping
    @Operation(summary = "Get all client's accounts", description = "Returns a list of all client's accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account list"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account list retrieval failed")
    })
    public ResponseEntity<?> getMyAccounts(@RequestHeader("Authorization") String auth){
        try {
            Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
            return ResponseEntity.ok(accountService.getMyAccounts(clientId));
        }catch (UserNotAClientException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    //oVO MOZDA VISE I NIJE POTREBNO JER JE KOLEGA KOJI JE MERGOVAO PRE MENE PROSIRIO aCCOUNTdTO DA UKLJUCUJE
    //I ONO STO SAM JA RAZDVOJIO U AccountDetailsDto -- izvini za Caps
    @PreAuthorize("hasRole('CLIENT') or hasRole('EMPLOYEE') or hasRole('ADMIN')")
    @GetMapping("/details/{accountNumber}")
    @Operation(summary = "Get account details", description = "Returns account details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account with details"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account details retrieval failed")
    })
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String auth,
                                               @PathVariable("accountNumber") String accountNumber){
        try {
            Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
            return ResponseEntity.ok(accountService.getAccountDetails(clientId, accountNumber));
        }catch (UserNotAClientException | ClientNotAccountOwnerException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
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
