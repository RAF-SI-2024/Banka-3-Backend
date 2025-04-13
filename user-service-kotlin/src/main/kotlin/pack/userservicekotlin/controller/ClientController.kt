package pack.userservicekotlin.controller

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.ClientServiceError
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.service.ClientService
import pack.userservicekotlin.swagger.ClientApiDoc

@RestController
@RequestMapping("/api/admin/clients")
class ClientController(
    private val clientService: ClientService,
) : ClientApiDoc {
    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping
    override fun getAllClients(
        @RequestParam(required = false) firstName: String?,
        @RequestParam(required = false) lastName: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<ClientResponseDto>> {
        val pageable = PageRequest.of(page, size, Sort.by("lastName").ascending())
        return ResponseEntity.ok(clientService.listClientsWithFilters(firstName, lastName, email, pageable))
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/{id}")
    override fun getClientById(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        clientService.getClientById(id).fold(
            ifLeft = {
                when (it) {
                    is ClientServiceError.NotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found with ID: ${it.id}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    override fun addClient(
        @Valid @RequestBody createClientDto: CreateClientDto,
    ): ResponseEntity<Any> =
        clientService.addClient(createClientDto).fold(
            ifLeft = {
                when (it) {
                    is ClientServiceError.RoleNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client role not found")
                    ClientServiceError.InvalidInput -> ResponseEntity.badRequest().body("Invalid client data")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
        )

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/{id}")
    override fun updateClient(
        @PathVariable id: Long,
        @Valid @RequestBody updateClientDto: UpdateClientDto,
    ): ResponseEntity<Any> =
        clientService.updateClient(id, updateClientDto).fold(
            ifLeft = {
                when (it) {
                    is ClientServiceError.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasRole('EMPLOYEE')")
    @DeleteMapping("/{id}")
    override fun deleteClient(
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        clientService.deleteClient(id).fold(
            ifLeft = { ResponseEntity.status(HttpStatus.NOT_FOUND).build() },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/me")
    override fun getCurrentClient(): ResponseEntity<Any> =
        clientService.getCurrentClient().fold(
            ifLeft = {
                when (it) {
                    is ClientServiceError.EmailNotFound ->
                        ResponseEntity
                            .status(
                                HttpStatus.NOT_FOUND,
                            ).body("Client not found with email: ${it.email}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )
}
