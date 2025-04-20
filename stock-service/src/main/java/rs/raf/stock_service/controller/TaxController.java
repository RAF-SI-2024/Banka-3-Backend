package rs.raf.stock_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.stock_service.domain.dto.ErrorMessageDto;
import rs.raf.stock_service.domain.dto.TrackedPaymentDto;
import rs.raf.stock_service.exceptions.InsufficientFundsException;
import rs.raf.stock_service.service.TaxService;

@Tag(name = "Tax API", description = "Api for managing taxes")
@RestController
@RequestMapping("/api/tax")
@AllArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    @Operation(summary = "Get taxes", description = "Returns clients, actuaries and their unpaid taxes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Taxes obtained successfully"),
    })
    public ResponseEntity<?> getTaxes(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String role
    ) {
        return ResponseEntity.ok().body(taxService.getTaxes(name, surname, role));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping("/process")
    @Operation(summary = "Process taxes.", description = "Pays taxes where possible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
    })
    public ResponseEntity<?> processTaxes() {
        try {
            TrackedPaymentDto trackedPaymentDto = taxService.processTaxes();
            return ResponseEntity.status(HttpStatus.OK).body(trackedPaymentDto);

        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessageDto(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
