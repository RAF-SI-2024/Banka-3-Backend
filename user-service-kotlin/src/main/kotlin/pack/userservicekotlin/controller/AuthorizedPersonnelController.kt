package pack.userservicekotlin.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.AuthorizedPersonnelServiceError
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.service.AuthorizedPersonnelService
import pack.userservicekotlin.swagger.AuthorizedPersonnelApiDoc

@RestController
@RequestMapping("/api/authorized-personnel")
@Tag(name = "Authorized Personnel Management", description = "API for managing authorized personnel for companies")
class AuthorizedPersonnelController(
    private val authorizedPersonnelService: AuthorizedPersonnelService,
) : AuthorizedPersonnelApiDoc {
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @PostMapping
    override fun createAuthorizedPersonnel(
        @Valid @RequestBody createAuthorizedPersonnelDto: CreateAuthorizedPersonnelDto,
    ): ResponseEntity<Any> =
        authorizedPersonnelService.createAuthorizedPersonnel(createAuthorizedPersonnelDto).fold(
            ifLeft = {
                when (it) {
                    is AuthorizedPersonnelServiceError.CompanyNotFound ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Company not found with id: ${it.companyId}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.status(HttpStatus.CREATED).body(it) },
        )

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @GetMapping("/company/{companyId}")
    override fun getAuthorizedPersonnelByCompany(
        @PathVariable companyId: Long,
    ): ResponseEntity<Any> =
        authorizedPersonnelService.getAuthorizedPersonnelByCompany(companyId).fold(
            ifLeft = {
                when (it) {
                    is AuthorizedPersonnelServiceError.CompanyNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company not found with id: ${it.companyId}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @GetMapping("/{id}")
    override fun getAuthorizedPersonnelById(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        authorizedPersonnelService.getAuthorizedPersonnelById(id).fold(
            ifLeft = {
                when (it) {
                    is AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Authorized personnel not found with id: ${it.id}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @PutMapping("/{id}")
    override fun updateAuthorizedPersonnel(
        @PathVariable id: Long,
        @Valid @RequestBody updateDto: CreateAuthorizedPersonnelDto,
    ): ResponseEntity<Any> =
        authorizedPersonnelService.updateAuthorizedPersonnel(id, updateDto).fold(
            ifLeft = {
                when (it) {
                    is AuthorizedPersonnelServiceError.CompanyNotFound ->
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Company not found with id: ${it.companyId}")
                    is AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Authorized personnel not found with id: ${it.id}")
                }
            },
            ifRight = { ResponseEntity.ok(it) },
        )

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @DeleteMapping("/{id}")
    override fun deleteAuthorizedPersonnel(
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        authorizedPersonnelService.deleteAuthorizedPersonnel(id).fold(
            ifLeft = {
                when (it) {
                    is AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.noContent().build() },
        )
}
