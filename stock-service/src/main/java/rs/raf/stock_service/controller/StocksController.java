package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.StockDto;
import rs.raf.stock_service.service.StocksService;

@Tag(name = "Stocks API", description = "Api for managing stocks")
@RestController
@RequestMapping("/api/stocks")
public class StocksController {

    private final StocksService stocksService;

    public StocksController(StocksService stocksService) {
        this.stocksService = stocksService;
    }

    @Operation(summary = "Search stocks by ticker keywords")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results"),
            @ApiResponse(responseCode = "404", description = "No stocks found for the given keyword"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchByTicker(@RequestParam("keywords") String keyword) {
        return ResponseEntity.ok(stocksService.searchByTicker(keyword));
    }

    @Operation(summary = "Get stock details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved stock details"),
            @ApiResponse(responseCode = "404", description = "Stock data not found for the given symbol"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStock(@PathVariable String symbol) {
        return ResponseEntity.ok(stocksService.getStockData(symbol));
    }

    @Operation(summary = "Get paginated list of stocks")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list of stocks"),
            @ApiResponse(responseCode = "404", description = "No stocks found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public ResponseEntity<?> getAllStocks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<StockDto> stocks = stocksService.getStocksList(PageRequest.of(page, size));
        return ResponseEntity.ok(stocks);
    }
}
