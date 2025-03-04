package rs.raf.bank_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.dto.ChangeAccountLimitDto;
import rs.raf.bank_service.dto.ChangeAccountNameDto;
import rs.raf.bank_service.service.AccountService;

import javax.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PreAuthorize("hasAuthority('CLIENT')")
    @PutMapping("/{id}/change-name")
    public ResponseEntity<?> changeAccountName(@PathVariable Long id,
                                               @RequestBody @Valid ChangeAccountNameDto request) {
        accountService.changeAccountName(id, request.getNewName());
        return ResponseEntity.ok("Account name updated successfully");
    }

    @PreAuthorize("hasAuthority('CLIENT')")
    @PutMapping("/{id}/change-limit")
    public ResponseEntity<?> changeAccountLimit(@PathVariable Long id,
                                                @RequestBody @Valid ChangeAccountLimitDto request) {
        accountService.changeAccountLimit(id, request.getNewLimit());
        return ResponseEntity.ok("Account limit updated successfully");
    }

    //  Dodaj ExceptionHandler za IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
