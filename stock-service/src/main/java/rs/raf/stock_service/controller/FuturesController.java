package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.FuturesContractDto;
import rs.raf.stock_service.service.FuturesService;

import java.util.List;

@Tag(name = "Futures API", description = "Operations related to futures contracts loaded from CSV file")
@RestController
@AllArgsConstructor
@RequestMapping("/api/futures")
public class FuturesController {

    private final FuturesService futuresService;

    @Operation(summary = "Get all futures contracts", description = "Returns a list of futures contracts loaded from the CSV file with derived maintenance margin")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved futures contracts"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public ResponseEntity<List<FuturesContractDto>> getAllFuturesContracts() {
        List<FuturesContractDto> contracts = futuresService.getFuturesContracts();
        return ResponseEntity.ok(contracts);
    }

    @Operation(summary = "Get a specific futures contract by ticker", description = "Returns a futures contract by its ticker")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved futures contract"),
            @ApiResponse(responseCode = "404", description = "Futures contract not found")
    })
    @GetMapping("/{ticker}")
    public ResponseEntity<FuturesContractDto> getFuturesContractByTicker(@PathVariable String ticker) {
        FuturesContractDto dto = futuresService.getFuturesContractByTicker(ticker);
        return ResponseEntity.ok(dto);
    }
}
