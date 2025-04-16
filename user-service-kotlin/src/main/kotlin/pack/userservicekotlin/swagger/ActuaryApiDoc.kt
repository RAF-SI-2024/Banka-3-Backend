package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.activity_code.SetApprovalDto
import pack.userservicekotlin.domain.dto.actuary_limit.UpdateActuaryLimitDto
import pack.userservicekotlin.domain.dto.employee.AgentDto

interface ActuaryApiDoc {
    @Operation(summary = "Change agent limit.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Agent limit changed successfully."),
        ApiResponse(responseCode = "404", description = "Agent not found"),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
    )
    @PutMapping("change-limit/{id}")
    fun changeAgentLimit(
        @Parameter(description = "Agent ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody changeActuaryLimitDto: UpdateActuaryLimitDto,
    ): ResponseEntity<*>

    @Operation(summary = "Reset daily limit for an agent.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Agent daily limit reset successfully."),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
    )
    @PutMapping("reset-limit/{id}")
    fun resetDailyLimit(
        @Parameter(description = "Agent ID", required = true) @PathVariable id: Long,
    ): ResponseEntity<*>

    @Operation(summary = "Set approval value for an agent.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Agent approval value set successfully."),
        ApiResponse(responseCode = "400", description = "Invalid input data"),
    )
    @PutMapping("set-approval/{id}")
    fun setApprovalValue(
        @Parameter(description = "Agent ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody setApprovalDto: SetApprovalDto,
    ): ResponseEntity<*>

    @Operation(summary = "Get all agents with filtering.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Agents retrieved successfully"),
    )
    @GetMapping
    fun getAllAgents(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) position: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<AgentDto>>

    @Operation(summary = "Get agent actuary limit.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Agent actuary limit returned successfully."),
        ApiResponse(responseCode = "404", description = "Not found."),
    )
    @GetMapping("{agentId}")
    fun getAgentLimit(
        @Parameter(description = "Agent ID", required = true) @PathVariable agentId: Long,
    ): ResponseEntity<*>
}
