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
import rs.raf.stock_service.domain.dto.OptionDto;
import rs.raf.stock_service.domain.enums.OptionType;
import rs.raf.stock_service.service.OptionService;

import java.util.List;

@Tag(name = "Options API", description = "Operations for generating options based on underlying stock price")
@RestController
@AllArgsConstructor
@RequestMapping("/api/options")
public class OptionsController {

    private final OptionService optionService;

    @Operation(summary = "Get all options", description = "Returns a list of all option listings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved options"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/all")
    public ResponseEntity<List<OptionDto>> getAllOptions() {
        List<OptionDto> options = optionService.getAllOptions();
        return ResponseEntity.ok(options);
    }

    @Operation(summary = "Get specific option by ticker", description = "Returns an option listing by its ticker")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved option"),
            @ApiResponse(responseCode = "404", description = "Option not found")
    })
    @GetMapping("/{ticker}")
    public ResponseEntity<OptionDto> getOptionByTicker(@PathVariable String ticker) {
        OptionDto dto = optionService.getOptionByTicker(ticker);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get options by type", description = "Returns a list of options filtered by OptionType (CALL or PUT)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered options"),
            @ApiResponse(responseCode = "400", description = "Invalid option type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<OptionDto>> getOptionsByType(@PathVariable String type) {
        try {
            OptionType optionType = OptionType.valueOf(type.toUpperCase());
            List<OptionDto> options = optionService.getOptionsByType(optionType);
            return ResponseEntity.ok(options);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

}
