package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.*;
import rs.raf.user_service.exceptions.ActuaryLimitNotFoundException;
import rs.raf.user_service.exceptions.EmployeeNotFoundException;
import rs.raf.user_service.exceptions.UserNotAgentException;
import rs.raf.user_service.service.ActuaryService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/actuaries")
@Tag(name = "Actuary Management", description = "API for managing actuaries")
@AllArgsConstructor
public class ActuaryController {

    private final ActuaryService actuaryService;

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("change-limit/{id}")
    @Operation(summary = "Change agent limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent limit changed successfully."),
            @ApiResponse(responseCode = "404", description = "Agent not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> changeAgentLimit(@PathVariable Long id, @Valid @RequestBody ChangeAgentLimitDto changeAgentLimitDto) {
        try {
            actuaryService.changeAgentLimit(id, changeAgentLimitDto.getNewLimit());
            return ResponseEntity.ok().build();
        } catch (ActuaryLimitNotFoundException | EmployeeNotFoundException | UserNotAgentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("reset-limit/{id}")
    @Operation(summary = "Reset daily limit for an agent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent daily limit reset successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> resetDailyLimit(@PathVariable Long id) {
        try {
            actuaryService.resetDailyLimit(id);
            return ResponseEntity.ok().build();
        } catch (ActuaryLimitNotFoundException | EmployeeNotFoundException | UserNotAgentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("set-approval/{id}")
    @Operation(summary = "Set approval value for an agent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent approval value set successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<?> setApprovalValue(@PathVariable Long id, @Valid @RequestBody SetApprovalDto setApprovalDto) {
        try {
            actuaryService.setApproval(id, setApprovalDto.getNeedApproval());
            return ResponseEntity.ok().build();
        } catch (ActuaryLimitNotFoundException | EmployeeNotFoundException | UserNotAgentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/agents")
    @Operation(summary = "Get all agents with filtering.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agents retrieved successfully")
    })
    public ResponseEntity<Page<EmployeeDto>> getAllAgents(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String position,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actuaryService.findAgents(firstName, lastName, email, position, pageable));
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    @Operation(summary = "Get all actuaries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actuaries retrieved successfully")
    })
    public ResponseEntity<Page<ActuaryDto>> getAllActuaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(actuaryService.findActuaries(pageable));
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','AGENT')")
    @GetMapping("{agentId}")
    @Operation(summary = "Get agent actuary limit.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent actuary limit returned successfully."),
            @ApiResponse(responseCode = "404", description = "Not found.")
    })
    public ResponseEntity<?> getAgentLimit(@PathVariable Long agentId) {
        try {
            return ResponseEntity.ok().body(actuaryService.getAgentLimit(agentId));
        } catch (ActuaryLimitNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllAgentsAndClients(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String  role
    ){
        return ResponseEntity.ok().body(actuaryService.getAllAgentsAndClients(name,surname,role));
    }
}
