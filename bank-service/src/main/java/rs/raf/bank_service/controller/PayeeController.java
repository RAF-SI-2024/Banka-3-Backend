package rs.raf.bank_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import rs.raf.bank_service.domain.dto.PayeeDto;
import rs.raf.bank_service.exceptions.PayeeNotFoundException;
import rs.raf.bank_service.service.PayeeService;
import rs.raf.bank_service.utils.JwtTokenUtil;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "Payees Controller", description = "API for managing payees")
@Validated
@RestController
@RequestMapping("/api/payees")
@AllArgsConstructor
public class PayeeController {

    private final PayeeService service;
    private final JwtTokenUtil jwtTokenUtil;

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Add a new payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payee created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<String> createPayee(
            @Valid @RequestBody PayeeDto dto,
            @RequestHeader("Authorization") String auth) {

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        service.create(dto, clientId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Payee created successfully.");
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @GetMapping("/client")
    @Operation(summary = "Get all payees for the authenticated client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payees retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PayeeDto>> getPayeesByClientId(@RequestHeader("Authorization") String auth) {
        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        List<PayeeDto> payees = service.getByClientId(clientId);
        return ResponseEntity.ok(payees);
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payee updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Payee not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<String> updatePayee(
            @PathVariable Long id,
            @Valid @RequestBody PayeeDto dto,
            @RequestHeader("Authorization") String auth) {

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        try {
            service.update(id, dto, clientId);
            return ResponseEntity.ok("Payee updated successfully.");
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("(isAuthenticated() and hasRole('CLIENT')) or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payee.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payee not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deletePayee(
            @PathVariable Long id,
            @RequestHeader("Authorization") String auth) {

        Long clientId = jwtTokenUtil.getUserIdFromAuthHeader(auth);
        try {
            service.delete(id, clientId);
            return ResponseEntity.noContent().build();
        } catch (PayeeNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
