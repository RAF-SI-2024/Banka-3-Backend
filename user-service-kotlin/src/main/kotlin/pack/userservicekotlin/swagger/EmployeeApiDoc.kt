package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto

interface EmployeeApiDoc {
    @Operation(summary = "Get employee by ID", description = "Returns an employee based on the provided ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully retrieved employee"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @GetMapping("/{id}")
    fun getEmployeeById(
        @Parameter(description = "Employee ID") @PathVariable id: Long,
    ): ResponseEntity<Any>

    @Operation(summary = "Get all employees", description = "Returns a paginated list of employees with optional filters")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully retrieved employee list"),
    )
    @GetMapping
    fun getAllEmployees(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) position: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<EmployeeResponseDto>>

    @Operation(summary = "Create an employee", description = "Creates an employee.")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Employee created successfully"),
        ApiResponse(responseCode = "400", description = "Input values in wrong format"),
        ApiResponse(responseCode = "500", description = "Employee creation failed"),
    )
    @PostMapping
    fun createEmployee(
        @RequestBody createEmployeeDTO: CreateEmployeeDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Update an employee", description = "Updates an employee.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee updated successfully"),
        ApiResponse(responseCode = "400", description = "Input values in wrong format"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
        ApiResponse(responseCode = "500", description = "Employee update failed"),
    )
    @PutMapping("/{id}")
    fun updateEmployee(
        @Parameter(description = "Employee ID") @PathVariable id: Long,
        @RequestBody updateEmployeeDTO: UpdateEmployeeDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Delete an employee", description = "Deletes an employee by their ID.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee deleted successfully"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @DeleteMapping("/{id}")
    fun deleteEmployee(
        @Parameter(description = "Employee ID") @PathVariable id: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "Deactivate an employee", description = "Deactivates an employee by their ID.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee deactivated successfully"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @PatchMapping("/{id}/deactivate")
    fun deactivateEmployee(
        @Parameter(description = "Employee ID") @PathVariable id: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "Activate an employee", description = "Activates an employee by their ID.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Employee activated successfully"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @PatchMapping("/{id}/activate")
    fun activateEmployee(
        @Parameter(description = "Employee ID") @PathVariable id: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "Get current employee", description = "Returns the currently authenticated employee's details")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully retrieved employee details"),
        ApiResponse(responseCode = "404", description = "Employee not found"),
    )
    @GetMapping("/me")
    fun getCurrentEmployee(): ResponseEntity<Any>
}
