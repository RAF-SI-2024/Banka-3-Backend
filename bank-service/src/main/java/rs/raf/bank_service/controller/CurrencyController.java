package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.bank_service.domain.dto.CurrencyDto;
import rs.raf.bank_service.service.CurrencyService;

@Tag(name = "Currency controller", description = "API for managing currencies")
@RestController
@RequestMapping("/currency")
public class CurrencyController {
    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Create a new currency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Currency created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<CurrencyDto> createCurrency(@RequestBody CurrencyDto currency) {
        return ResponseEntity.ok(currencyService.createCurrency(currency));
    }
}
