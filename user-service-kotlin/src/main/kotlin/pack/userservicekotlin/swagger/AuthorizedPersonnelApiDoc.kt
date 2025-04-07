package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto

@Tag(name = "Authorized Personnel Management", description = "API for managing authorized personnel for companies")
interface AuthorizedPersonnelApiDoc {
    @Operation(summary = "Create new authorized personnel", description = "Creates new authorized personnel for a company")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Successfully created authorized personnel"),
            ApiResponse(responseCode = "400", description = "Invalid data"),
            ApiResponse(responseCode = "403", description = "Not authorized to create authorized personnel for this company"),
        ],
    )
    @PostMapping
    fun createAuthorizedPersonnel(
        @Valid @RequestBody createAuthorizedPersonnelDto: CreateAuthorizedPersonnelDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Get authorized personnel for company", description = "Gets all authorized personnel for a company")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved authorized personnel"),
            ApiResponse(responseCode = "404", description = "Company not found"),
        ],
    )
    @GetMapping("/company/{companyId}")
    fun getAuthorizedPersonnelByCompany(
        @PathVariable companyId: Long,
    ): ResponseEntity<Any>

    @Operation(summary = "Get authorized personnel by ID", description = "Gets an authorized personnel by ID")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved authorized personnel"),
            ApiResponse(responseCode = "404", description = "Authorized personnel not found"),
        ],
    )
    @GetMapping("/{id}")
    fun getAuthorizedPersonnelById(
        @PathVariable id: Long,
    ): ResponseEntity<Any>

    @Operation(summary = "Update authorized personnel", description = "Updates an authorized personnel")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully updated authorized personnel"),
            ApiResponse(responseCode = "400", description = "Invalid data"),
            ApiResponse(responseCode = "403", description = "Not authorized to update authorized personnel for this company"),
            ApiResponse(responseCode = "404", description = "Authorized personnel not found"),
        ],
    )
    @PutMapping("/{id}")
    fun updateAuthorizedPersonnel(
        @PathVariable id: Long,
        @Valid @RequestBody updateDto: CreateAuthorizedPersonnelDto,
    ): ResponseEntity<Any>

    @Operation(summary = "Delete authorized personnel", description = "Deletes an authorized personnel")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Successfully deleted authorized personnel"),
            ApiResponse(responseCode = "403", description = "Not authorized to delete authorized personnel for this company"),
            ApiResponse(responseCode = "404", description = "Authorized personnel not found"),
        ],
    )
    @DeleteMapping("/{id}")
    fun deleteAuthorizedPersonnel(
        @PathVariable id: Long,
    ): ResponseEntity<Void>
}
