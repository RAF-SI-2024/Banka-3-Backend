package pack.userservicekotlin.controller

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
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
import pack.userservicekotlin.domain.dto.employee.AgentDto
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
        actuaryService.changeAgentLimit(id, changeActuaryLimitDto.newLimit).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.EmployeeNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee with id ${it.id} not found")
                    is ActuaryServiceError.NotAnAgent ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User with id ${it.employeeId} is not an agent")
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Limit not found for agent ${it.employeeId}")
                    is ActuaryServiceError.ExternalServiceError -> TODO()
                    ActuaryServiceError.InvalidPageRequest -> TODO()
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

                    is ActuaryServiceError.ExternalServiceError -> TODO()
                    ActuaryServiceError.InvalidPageRequest -> TODO()
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

                    is ActuaryServiceError.ExternalServiceError -> TODO()
                    ActuaryServiceError.InvalidPageRequest -> TODO()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

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

    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("update-used-limit/{id}")
    override fun updateUsedLimit(
        @PathVariable id: Long,
        @Valid @RequestBody changeActuaryLimitDto: UpdateActuaryLimitDto,
    ): ResponseEntity<*> =
        actuaryService.updateUsedLimit(id, changeActuaryLimitDto.newLimit).fold(
            ifLeft = { error ->
                when (error) {
                    is ActuaryServiceError.ActuaryLimitNotFound,
                    is ActuaryServiceError.EmployeeNotFound,
                    is ActuaryServiceError.NotAnAgent,
                    -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
                    is ActuaryServiceError.ExternalServiceError -> TODO()
                    ActuaryServiceError.InvalidPageRequest -> TODO()
                }
            },
            ifRight = { dto -> ResponseEntity.ok(dto) },
        )

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/agents")
    override fun getAllAgents(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) position: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<AgentDto>> {
        val pageable = PageRequest.of(page, size)
        return actuaryService.findAgents(firstName, lastName, email, position, pageable).fold(
            ifLeft = {
                when (it) {
                    is ActuaryServiceError.ActuaryLimitNotFound ->
                        ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(PageImpl(emptyList())) // could log or include message
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )
    }

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping
    override fun getAllActuaries(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<ActuaryResponseDto>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return actuaryService.findActuaries(pageable).fold(
            ifLeft = { ResponseEntity.internalServerError().build() },
            ifRight = { ResponseEntity.ok(it) },
        )
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    override fun getAllAgentsAndClients(
        @RequestParam(defaultValue = "") name: String,
        @RequestParam(defaultValue = "") surname: String,
        @RequestParam(defaultValue = "") role: String,
    ): ResponseEntity<Any> =
        actuaryService.getAllAgentsAndClients(name, surname, role).fold(
            ifLeft = { ResponseEntity.internalServerError().body("Failed to fetch agents and clients") },
            ifRight = { ResponseEntity.ok(it) },
        )
}
