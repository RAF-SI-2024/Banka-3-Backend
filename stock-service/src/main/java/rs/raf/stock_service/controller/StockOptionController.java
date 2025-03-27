package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.raf.stock_service.domain.dto.StockOptionDto;
import rs.raf.stock_service.service.StockOptionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class StockOptionController {

    @Autowired
    private StockOptionService stockOptionService;

    @PreAuthorize("hasAnyRole('CLIENT', 'SUPERVISOR', 'AGENT', 'EMPLOYEE', 'ADMIN')")
    @GetMapping("/{id}/options/{date}")
    @Operation(summary = "Get stock options by expiration date", description = "Returns a list of call and put options for a given stock and expiration date.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock options retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Stock options not found")
    })
    public ResponseEntity<List<StockOptionDto>> getStockOptionsByDate(@PathVariable Long id, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(stockOptionService.getStockOptionsByDate(id, date));
    }
}
