package pack.userservicekotlin.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.CompanyServiceError
import pack.userservicekotlin.domain.dto.company.CompanyResponseDto
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto
import pack.userservicekotlin.service.CompanyService
import pack.userservicekotlin.swagger.CompanyApiDoc

@RestController
@RequestMapping("/api/company")
class CompanyController(
    private val companyService: CompanyService,
) : CompanyApiDoc {
    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping
    override fun createCompany(
        @Valid @RequestBody createCompanyDto: CreateCompanyDto,
    ): ResponseEntity<Any> =
        companyService.createCompany(createCompanyDto).fold(
            ifLeft = { error ->
                when (error) {
                    is CompanyServiceError.OwnerNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Owner not found: ${error.ownerId}")

                    is CompanyServiceError.ActivityCodeNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Activity code not found: ${error.activityCodeId}")

                    is CompanyServiceError.RegistrationNumberExists ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body("Company registration number exists: ${error.regNum}")

                    is CompanyServiceError.TaxIdExists ->
                        ResponseEntity.status(HttpStatus.CONFLICT).body("Company tax ID exists: ${error.taxId}")

                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { dto ->
                ResponseEntity.status(HttpStatus.CREATED).body(dto)
            },
        )

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    override fun getCompanyById(
        @PathVariable id: Long,
    ): ResponseEntity<Any> =
        companyService.getCompanyById(id).fold(
            ifLeft = { error ->
                when (error) {
                    is CompanyServiceError.CompanyNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company not found with ID: ${error.companyId}")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { dto -> ResponseEntity.ok(dto) },
        )

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @GetMapping("/owned-by/{id}")
    override fun getCompaniesForClientId(
        @PathVariable id: Long,
    ): ResponseEntity<List<CompanyResponseDto>> = ResponseEntity.ok(companyService.getCompaniesForClientId(id))
}
