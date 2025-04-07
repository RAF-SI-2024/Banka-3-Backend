package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto

@Tag(name = "Client Management", description = "API for managing clients")
interface ClientApiDoc {
    @Operation(summary = "Get all clients with filtering and pagination")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Clients retrieved successfully"),
        ],
    )
    @GetMapping
    fun getAllClients(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<ClientResponseDto>>

    @Operation(summary = "Get client by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Client retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Client not found"),
        ],
    )
    @GetMapping("/{id}")
    fun getClientById(
        @PathVariable id: Long,
    ): ResponseEntity<Any>

    @Operation(summary = "Add new client (password is set during activation)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Client created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid input data"),
        ],
    )
    @PostMapping
    fun addClient(
        @Valid @RequestBody createClientDto: CreateClientDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Update client (only allowed fields)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Client updated successfully"),
            ApiResponse(responseCode = "404", description = "Client not found"),
            ApiResponse(responseCode = "400", description = "Invalid input data"),
        ],
    )
    @PutMapping("/{id}")
    fun updateClient(
        @PathVariable id: Long,
        @Valid @RequestBody updateClientDto: UpdateClientDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Delete client by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Client deleted successfully"),
            ApiResponse(responseCode = "404", description = "Client not found"),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteClient(
        @PathVariable id: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "Get current client", description = "Returns the currently authenticated client's details")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved client details"),
            ApiResponse(responseCode = "404", description = "Client not found"),
        ],
    )
    @GetMapping("/me")
    fun getCurrentClient(): ResponseEntity<Any>
}
