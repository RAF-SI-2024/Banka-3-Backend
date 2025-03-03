package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Add new bank account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<String> createBankAccount(@RequestHeader("Authorization") String authorizationHeader, @RequestBody NewBankAccountDto newBankAccountDto){
        try {
            accountService.createNewBankAccount(newBankAccountDto,authorizationHeader);
//            if(newBankAccountDto.isCreateCard()){
//                accountService.createCard...
//            }
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }catch (ClientNotFoundException | CurrencyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
