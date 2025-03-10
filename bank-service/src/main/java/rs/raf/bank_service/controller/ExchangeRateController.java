package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.exceptions.InvalidExchangeRateException;
import rs.raf.bank_service.service.ExchangeRateService;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;


    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get exchange rate", description = "Returns the exchange rate between two currencies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{fromCurrency}/{toCurrency}")
    public ResponseEntity<?> getExchangeRate(
            @PathVariable String fromCurrency,
            @PathVariable String toCurrency) {
        try {
            BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                    fromCurrency, toCurrency);

            Map<String, Object> response = new HashMap<>();
            response.put("fromCurrency", fromCurrency);
            response.put("toCurrency", toCurrency);
            response.put("exchangeRate", exchangeRate);

            return ResponseEntity.ok(response);

        } catch (ExchangeRateNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }


    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add or update exchange rate", description = "Stores a new exchange rate or updates an existing one")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange rate saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid exchange rate input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> saveOrUpdateExchangeRate(@Valid @RequestBody ExchangeRateDto exchangeRateDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", errors));
        }


        return ResponseEntity.ok().body("Exchange rate saved successfully");
    }

}
