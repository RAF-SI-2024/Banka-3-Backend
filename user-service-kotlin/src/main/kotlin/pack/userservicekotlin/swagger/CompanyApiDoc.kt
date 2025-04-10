package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.company.CompanyResponseDto
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto

@Tag(name = "Company Management", description = "API for managing companies")
interface CompanyApiDoc {
    @Operation(summary = "Create new company", description = "Creates a new company with provided parameters")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Successfully created company"),
            ApiResponse(responseCode = "400", description = "Invalid data"),
        ],
    )
    @PostMapping
    fun createCompany(
        @Valid @RequestBody createCompanyDto: CreateCompanyDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Get company", description = "Retrieves company by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved company"),
            ApiResponse(responseCode = "404", description = "Company not found"),
            ApiResponse(responseCode = "500", description = "Company retrieval failed"),
        ],
    )
    @GetMapping("/{id}")
    fun getCompanyById(
        @PathVariable("id") id: Long,
    ): ResponseEntity<Any>

    @Operation(summary = "Get companies", description = "Retrieves companies by client ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved companies"),
            ApiResponse(responseCode = "500", description = "Company retrieval failed"),
        ],
    )
    @GetMapping("/owned-by/{id}")
    fun getCompaniesForClientId(
        @PathVariable("id") id: Long,
    ): ResponseEntity<List<CompanyResponseDto>>
}
