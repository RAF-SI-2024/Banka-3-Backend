package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.service.ExchangeRateService;

import javax.validation.Valid;

@Tag(name = "Exchange rate controller", description = "API for retrieving exchange rates")
@RestController
@RequestMapping("/api/exchange-rates")
@AllArgsConstructor
public class ExchangeRateController {

    private ExchangeRateService exchangeRateService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get the exchange rates", description = "Returns a list of all exchange rates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate")
    })
    public ResponseEntity<?> getExchangeRates(){
        return ResponseEntity.ok(exchangeRateService.getExchangeRates());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/convert")
    @Operation(summary = "Convert amount", description = "Converts the amount from one currency to another")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted")
    })
    public ResponseEntity<?> convert(@RequestBody @Valid ConvertDto convertDto){
        return ResponseEntity.ok(exchangeRateService.convert(convertDto));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{fromCurrency}/{toCurrency}")
    @Operation(summary = "Get exchange rate", description = "Returns the exchange rate between two currencies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully")
    })
    public ResponseEntity<?> getExchangeRate(@PathVariable String fromCurrency,
                                             @PathVariable String toCurrency) {
        ExchangeRateDto exchangeRateDto = exchangeRateService.getExchangeRate(fromCurrency, toCurrency);
        return ResponseEntity.ok(exchangeRateDto);
    }
}
