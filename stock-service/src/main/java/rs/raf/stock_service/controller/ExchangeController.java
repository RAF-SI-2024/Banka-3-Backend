package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.exceptions.ExchangesNotLoadedException;
import rs.raf.stock_service.service.ExchangeService;

import java.util.List;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {

    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        this.exchangeService = exchangeService;
    }

    // treba verovatno neki preauth staviti samo nisam znao koji

    @GetMapping
    @Operation(summary = "Get available exchanges.", description = "Get available exchanges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Available exchanges retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Exchanges not loaded properly.")
    })
    public ResponseEntity<?> getAvailableExchanges(){
        try {
            List<Exchange> availableExchanges = exchangeService.getAvailableExchanges();
            return ResponseEntity.ok(availableExchanges);
        }catch(ExchangesNotLoadedException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping
    @Operation(summary = "Toggle test mode.", description = "Toggle test mode.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully toggled test mode."),
            @ApiResponse(responseCode = "404", description = "Exchanges not loaded properly.")
    })
    public ResponseEntity<?> toggleTestMode(){
        try {
            exchangeService.toggleTestMode();
            return ResponseEntity.ok("Successfully toggled test mode.");
        }catch (ExchangesNotLoadedException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}