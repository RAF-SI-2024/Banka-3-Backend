package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.CreditDetailedDto;
import rs.raf.bank_service.domain.dto.CreditShortDto;
import rs.raf.bank_service.service.CreditService;

import java.util.List;
import java.util.Optional;

@Tag(name = "Credit controller", description = "API for managing credits")
@RestController
@RequestMapping("/credits")
public class CreditController {
    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PreAuthorize("hasAuthority('employee')")
    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get all credits by account number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully fetched credits"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<List<CreditShortDto>> getCreditsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditService.getCreditsByAccountNumber(accountNumber));
    }

    @PreAuthorize("hasAuthority('employee')")
    @GetMapping("/{id}")
    @Operation(summary = "Get a credit by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully fetched credit"),
            @ApiResponse(responseCode = "404", description = "Credit not found")
    })
    public ResponseEntity<CreditDetailedDto> getCreditById(@PathVariable Long id) {
        Optional<CreditDetailedDto> credit = creditService.getCreditById(id);
        return credit.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Create a new credit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<CreditDetailedDto> createCredit(@RequestBody CreditDetailedDto credit) {
        return ResponseEntity.ok(creditService.createCredit(credit));
    }
}
