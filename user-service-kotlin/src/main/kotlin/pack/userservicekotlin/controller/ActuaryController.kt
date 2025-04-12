package pack.userservicekotlin.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.ActuaryServiceError
import pack.userservicekotlin.domain.dto.activity_code.SetApprovalDto
import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryResponseDto
import pack.userservicekotlin.domain.dto.actuary_limit.UpdateActuaryLimitDto
import pack.userservicekotlin.service.ActuaryService
import pack.userservicekotlin.swagger.ActuaryApiDoc

@RestController
@RequestMapping("/api/admin/actuaries")
class ActuaryController(
    private val actuaryService: ActuaryService,
) : ActuaryApiDoc {
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("change-limit/{id}")
    override fun changeAgentLimit(
        @PathVariable id: Long,
        @Valid @RequestBody changeActuaryLimitDto: UpdateActuaryLimitDto,
    ): ResponseEntity<Any> =
        actuaryService.changeAgentLimit(id, changeActuaryLimitDto.newLimit!!).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.EmployeeNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee with id ${it.id} not found")
                    is ActuaryServiceError.NotAnAgent ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with id ${it.employeeId} is not an agent")
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found for agent ${it.employeeId}")
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("reset-limit/{id}")
    override fun resetDailyLimit(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        actuaryService.resetDailyLimit(id).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.EmployeeNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee with id ${it.id} not found")
                    is ActuaryServiceError.NotAnAgent ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with id ${it.employeeId} is not an agent")
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found for agent ${it.employeeId}")
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("set-approval/{id}")
    override fun setApprovalValue(
        @PathVariable id: Long,
        @Valid @RequestBody setApprovalDto: SetApprovalDto,
    ): ResponseEntity<Any> =
        actuaryService.setApproval(id, setApprovalDto.needApproval!!).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.EmployeeNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee with id ${it.id} not found")
                    is ActuaryServiceError.NotAnAgent ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with id ${it.employeeId} is not an agent")
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found for agent ${it.employeeId}")
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/agents")
    override fun getAllAgents(
        email: String?,
        firstName: String?,
        lastName: String?,
        position: String?,
        page: Int,
        size: Int,
    ): ResponseEntity<Page<AgentDto>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return actuaryService.findAgents(firstName, lastName, email, position, pageable).fold(
            ifLeft = { ResponseEntity.internalServerError().build() },
            ifRight = { ResponseEntity.ok(it) },
        )
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    @Operation(summary = "Get all actuaries.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Actuaries retrieved successfully"),
    )
    fun getAllActuaries(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<ActuaryResponseDto>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return actuaryService.findActuaries(pageable).fold(
            ifLeft = { ResponseEntity.internalServerError().build() },
            ifRight = { ResponseEntity.ok(it) },
        )
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','AGENT')")
    @GetMapping("{agentId}")
    override fun getAgentLimit(
        @PathVariable agentId: Long,
    ): ResponseEntity<Any> =
        actuaryService.getAgentLimit(agentId).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found for agent ${it.employeeId}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    fun getAllAgentsAndClients(
        @RequestParam(defaultValue = "") name: String?,
        @RequestParam(defaultValue = "") surname: String?,
        @RequestParam(defaultValue = "") role: String?,
    ): ResponseEntity<Any> =
        actuaryService.getAllAgentsAndClients(name, surname, role).fold(
            ifLeft = { ResponseEntity.internalServerError().body("Failed to fetch agents and clients") },
            ifRight = { ResponseEntity.ok(it) },
        )
}
