package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import rs.raf.bank_service.domain.dto.AccountDto;

import rs.raf.bank_service.domain.dto.NewBankAccountDto;
import rs.raf.bank_service.exceptions.ClientNotAccountOwnerException;
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
import rs.raf.bank_service.service.AccountService;

@Tag(name = "Bank accounts controller", description = "API for managing bank accounts")
@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;


    /// GET endpoint sa opcionalnim filterima i paginacijom/sortiranjem po prezimenu vlasnika
    @PreAuthorize("hasAuthority('admin')")
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


    @PreAuthorize("hasAuthority('employee')")
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all client's accounts", description = "Returns a list of all client's accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account list"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account list retrieval failed")
    })
    public ResponseEntity<?> getMyAccounts(@RequestHeader("Authorization") String authorizationHeader){
        try {
            return ResponseEntity.ok(accountService.getMyAccounts(authorizationHeader));
        }catch (UserNotAClientException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    //oVO MOZDA VISE I NIJE POTREBNO JER JE KOLEGA KOJI JE MERGOVAO PRE MENE PROSIRIO aCCOUNTdTO DA UKLJUCUJE
    //I ONO STO SAM JA RAZDVOJIO U AccountDetailsDto -- izvini za Caps
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account details", description = "Returns account details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account with details"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account details retrieval failed")
    })
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String authorizationHeader,
                                               @PathVariable("accountNumber") String accountNumber){
        try {
            return ResponseEntity.ok(accountService.getAccountDetails(authorizationHeader, accountNumber));
        }catch (UserNotAClientException | ClientNotAccountOwnerException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    //Ovo je za kada se klikne na racun da prikaze sve njegove transakcije (naznaceno da nije isto kao kada se klikne detalji)
    //Verovatno ce ovo ici u TransactionController ali nzm kako treba da bude jer nemam Transaction Entitet!!!!
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions/{accountNumber}")
    @Operation(summary = "Get account transactions", description = "Returns account transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved account transactions"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Account transaction retrieval failed")
    })
    public ResponseEntity<?> getAccountTransactions(@RequestHeader("Authorization") String authorizationHeader,
                                               @PathVariable("accountNumber") String accountNumber){
        try {
            return ResponseEntity.ok(null);
            //return ResponseEntity.ok(accountService.getAccountTransactions(authorizationHeader, accountNumber));
        }catch (UserNotAClientException | ClientNotAccountOwnerException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (RuntimeException e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
