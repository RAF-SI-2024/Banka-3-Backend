package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.exceptions.UserNotAClientException;
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
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate"),
            @ApiResponse(responseCode = "500", description = "Exchange rates retrieval failed")
    })
    public ResponseEntity<?> getExchangeRates(){
        try {
            return ResponseEntity.ok(exchangeRateService.getExchangeRates());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/convert")
    @Operation(summary = "Convert amount", description = "Converts the amount from one currency to another")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully converted"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Conversion failed")
    })
    public ResponseEntity<?> convert(@RequestBody @Valid ConvertDto convertDto){
        try {
            return ResponseEntity.ok(exchangeRateService.convert(convertDto));
        }catch (ExchangeRateNotFoundException |CurrencyNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
