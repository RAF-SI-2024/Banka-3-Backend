package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.exceptions.PayeesNotFoundByClientIdException;
import rs.raf.bank_service.service.PayeeService;

import java.util.List;

@Tag(name = "Payees Controller", description = "API for managing payees")
@Validated
@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeService service;

    public PayeeController(PayeeService service) {
        this.service = service;
    }

    @PreAuthorize("hasAuthority('client')")
    @PostMapping
    @Operation(summary = "Add a new payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payee created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<String> createPayee(@Valid @RequestBody PayeeDto dto) {
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Payee created successfully.");
    }

    @PreAuthorize("hasAuthority('client')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payee updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<String> updatePayee(@PathVariable Long id, @Valid @RequestBody PayeeDto dto) {
        try {
            service.update(id, dto);
            return ResponseEntity.ok("Payee updated successfully.");
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('client')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payee not found")
    })
    public ResponseEntity<Void> deletePayee(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasAuthority('client')")
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get all payees for a specific client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payees retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No payees found for this client")
    })
    public ResponseEntity<?> getPayeesByClientId(@PathVariable Long clientId) {
        try {
            List<PayeeDto> payees = service.getByClientId(clientId);
            return ResponseEntity.ok(payees);
        } catch (PayeesNotFoundByClientIdException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());  // Vraća 404 sa porukom
        }
    }

    @PreAuthorize("hasAuthority('client')")
    @GetMapping
    @Operation(summary = "Get all payees.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payees retrieved successfully")
    })
    public ResponseEntity<List<PayeeDto>> getAllPayees() {
        List<PayeeDto> payees = service.getAll();
        return ResponseEntity.ok(payees);
    }
}
