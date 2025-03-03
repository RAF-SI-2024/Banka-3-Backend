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
import rs.raf.bank_service.exceptions.ClientNotFoundException;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
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
}
