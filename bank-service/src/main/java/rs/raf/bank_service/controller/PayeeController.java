package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.service.PayeeService;

import java.util.stream.Collectors;

@Tag(name = "Payees Controller", description = "API for managing payees")
@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeService service;

    public PayeeController(PayeeService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('employee')")
    @PostMapping
    @Operation(summary = "Add a new payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payee created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<String> createPayee(@RequestHeader("Authorization") String authorizationHeader,
                                              @RequestBody PayeeDto dto,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Payee created successfully.");
    }

    @PreAuthorize("hasAuthority('employee')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payee updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<String> updatePayee(@PathVariable Long id,
                                              @RequestBody PayeeDto dto,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        try {
            service.update(id, dto);
            return ResponseEntity.ok("Payee updated successfully.");
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('employee')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<String> deletePayee(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}

