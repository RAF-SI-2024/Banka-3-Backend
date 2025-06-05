package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.ErrorMessageDto;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.exceptions.ExchangesNotLoadedException;
import rs.raf.stock_service.service.ExchangeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    // treba verovatno neki preauth staviti samo nisam znao koji

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get available exchanges.", description = "Get available exchanges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available exchanges retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchanges not loaded properly.")
    })
    public ResponseEntity<?> getAvailableExchanges() {
        try {
            return ResponseEntity.ok(exchangeService.getAvailableExchangesDto());
        } catch (ExchangesNotLoadedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping
    @Operation(summary = "Toggle test mode.", description = "Toggle test mode.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully toggled test mode."),
            @ApiResponse(responseCode = "404", description = "Exchanges not loaded properly.")
    })
    public ResponseEntity<?> toggleTestMode() {
        try {
            exchangeService.toggleTestMode();
            return ResponseEntity.ok(Map.of());
        } catch (ExchangesNotLoadedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessageDto(e.getMessage()));
        }
    }
}
