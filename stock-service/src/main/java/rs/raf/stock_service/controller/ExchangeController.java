package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.ConversionResponseDto;
import rs.raf.stock_service.service.ForexService;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "Exchange API", description = "Operations for currency conversion and retrieving latest rates")
@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    @Autowired
    private ForexService forexService;

    @Operation(summary = "Convert currency", description = "Converts an amount from base currency to target currency. If no amount is provided, returns only the conversion rate.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversion successful"),
            @ApiResponse(responseCode = "500", description = "Internal server error during conversion")
    })
    @GetMapping("/convert")
    public ResponseEntity<ConversionResponseDto> convertCurrency(
            @RequestParam("base") String base,
            @RequestParam("target") String target,
            @RequestParam(value = "amount", required = false) BigDecimal amount) {

        BigDecimal conversionRate = forexService.getConversionRate(base, target);
        ConversionResponseDto dto = new ConversionResponseDto();
        dto.setConversionRate(conversionRate);
        if (amount != null) {
            dto.setConvertedAmount(amount.multiply(conversionRate));
        }
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get latest conversion rates", description = "Returns a map of conversion rates for the given base currency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Latest rates retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestRates(@RequestParam("base") String base) {
        return ResponseEntity.ok(forexService.getLatestRates(base));
    }
}
