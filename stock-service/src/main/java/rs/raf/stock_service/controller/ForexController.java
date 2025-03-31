package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.ForexPairDto;
import rs.raf.stock_service.service.ForexService;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "Forex API", description = "Api for managing forex pairs")
@RestController
@RequestMapping("/api/forex")
@AllArgsConstructor
public class ForexController {

    private final ForexService forexService;

    @Operation(summary = "Get forex pair details", description = "Returns detailed forex pair data for given base and quote currencies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved forex pair details"),
            @ApiResponse(responseCode = "404", description = "Forex pair data not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<?> getForex(@RequestParam("base") String base, @RequestParam("quote") String quote) {
        return ResponseEntity.ok(forexService.getForexPair(base, quote));
    }

    @Operation(summary = "Convert currency", description = "Returns conversion rate and converted amount if provided.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved conversion rate and amount"),
            @ApiResponse(responseCode = "500", description = "Internal server error during currency conversion")
    })
    @GetMapping("/convert")
    public ResponseEntity<?> convertCurrency(
            @RequestParam("base") String base,
            @RequestParam("target") String target,
            @RequestParam(value = "amount", required = false) BigDecimal amount) {
        BigDecimal conversionRate = forexService.getConversionRate(base, target);
        Map<String, Object> response = Map.of("conversion_rate", conversionRate,
                "converted_amount", amount != null ? amount.multiply(conversionRate) : null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get latest forex rates", description = "Returns a map of conversion rates for the given base currency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved latest forex rates"),
            @ApiResponse(responseCode = "404", description = "Latest forex rates not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestRates(@RequestParam("base") String base) {
        return ResponseEntity.ok(forexService.getLatestRates(base));
    }

    @Operation(summary = "Get paginated list of forex pairs", description = "Returns a paginated list of forex pairs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list of forex pairs"),
            @ApiResponse(responseCode = "404", description = "No forex pairs found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public ResponseEntity<?> getAllForexPairs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ForexPairDto> forexPairs = forexService.getForexPairsList(PageRequest.of(page, size));
        return ResponseEntity.ok(forexPairs);
    }
}
