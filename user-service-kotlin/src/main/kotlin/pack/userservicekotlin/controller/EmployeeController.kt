package pack.userservicekotlin.controller

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.EmployeeServiceError
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.service.EmployeeService
import pack.userservicekotlin.swagger.EmployeeApiDoc

@RestController
@RequestMapping("/api/admin/employees")
class EmployeeController(
    private val employeeService: EmployeeService,
) : EmployeeApiDoc {
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{id}")
    override fun getEmployeeById(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        employeeService.findById(id).fold(
            ifLeft = {
                when (it) {
                    EmployeeServiceError.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping
    override fun getAllEmployees(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false)lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) position: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<EmployeeResponseDto>> {
        val pageRequest = PageRequest.of(page, size)
        return ResponseEntity.ok(employeeService.findAll(firstName, lastName, email, position, pageRequest))
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    override fun createEmployee(
        @RequestBody @Valid createEmployeeDTO: CreateEmployeeDto,
    ): ResponseEntity<Any> =
        employeeService.createEmployee(createEmployeeDTO).fold(
            ifLeft = {
                when (it) {
                    EmployeeServiceError.RoleNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found")
                    EmployeeServiceError.EmailAlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists")
                    EmployeeServiceError.JmbgAlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT).body("Jmbg already exists")
                    EmployeeServiceError.UsernameAlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists")

                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
        )

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    override fun updateEmployee(
        @PathVariable id: Long,
        @RequestBody @Valid updateEmployeeDTO: UpdateEmployeeDto,
    ): ResponseEntity<Any> =
        employeeService.updateEmployee(id, updateEmployeeDTO).fold(
            ifLeft = {
                when (it) {
                    EmployeeServiceError.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found")
                    EmployeeServiceError.RoleNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found")
                    EmployeeServiceError.LimitNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    override fun deleteEmployee(
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        employeeService.deleteEmployee(id).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).build() },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    override fun deactivateEmployee(
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        employeeService.deactivateEmployee(id).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).build() },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    override fun activateEmployee(
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        employeeService.activateEmployee(id).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).build() },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/me")
    override fun getCurrentEmployee(): ResponseEntity<Any> {
        val auth =
            SecurityContextHolder.getContext().authentication
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val email = auth.name
        return employeeService.findByEmail(email).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found") },
            ifRight = { ResponseEntity.ok(it) },
        )
    }
}
