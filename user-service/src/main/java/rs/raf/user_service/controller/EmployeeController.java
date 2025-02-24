package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.dto.CreateEmployeeDTO;
import rs.raf.user_service.dto.EmployeeDTO;
import rs.raf.user_service.dto.UpdateEmployeeDTO;
import rs.raf.user_service.service.EmployeeService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/employees")
@Tag(name = "Employee Management", description = "API for managing employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Get employee by ID", description = "Returns an employee based on the provided ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved employee"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/{id}")
    public EmployeeDTO getEmployeeById(@PathVariable Long id) {
        return employeeService.findById(id);
    }

    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Get all employees", description = "Returns a paginated list of employees with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved employee list")
    })
    @GetMapping
    public Page<EmployeeDTO> getAllEmployees(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return employeeService.findAll(position, department, active, pageable);
    }

    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Delete an employee", description = "Deletes an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Deactivate an employee", description = "Deactivates an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            employeeService.deactivateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PreAuthorize("hasAuthority('admin')")
    @Operation(summary = "Activate an employee", description = "Activates an employee by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee activated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            employeeService.activateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Create an employee", description = "Creates an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created successfully"),
            @ApiResponse(responseCode = "400", description = "Input values in wrong format"),
            @ApiResponse(responseCode = "500", description = "Employee creation failed")
    })
    @PostMapping
    public ResponseEntity<Void> createEmployee(
            @RequestBody @Valid CreateEmployeeDTO createEmployeeDTO,
            BindingResult result
    ){
        if (result.hasErrors())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        try {
            employeeService.createEmployee(createEmployeeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update an employee", description = "Updates an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "400", description = "Input values in wrong format"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "500", description = "Employee update failed")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEmployee(
            @Parameter(description = "Employee ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody @Valid UpdateEmployeeDTO updateEmployeeDTO,
            BindingResult result
    ) {
        if (result.hasErrors())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        try {
            employeeService.updateEmployee(id, updateEmployeeDTO);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch (EntityNotFoundException notFoundException){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
