package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.verification.CreateVerificationRequestDto

@Tag(name = "Verification Requests", description = "API for managing user verification requests")
interface VerificationRequestApiDoc {
    @Operation(summary = "Get active verification requests")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of active requests retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized access"),
    )
    @GetMapping("/active-requests")
    fun getActiveRequests(
        @RequestHeader("User-Agent", required = false) userAgent: String?,
    ): ResponseEntity<*>

    @Operation(summary = "Get request history")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of non pending requests retrieved successfully"),
        ApiResponse(responseCode = "401", description = "Unauthorized access"),
    )
    @GetMapping("/history")
    fun getRequestHistory(
        @RequestHeader("User-Agent", required = false) userAgent: String?,
    ): ResponseEntity<*>

    @Operation(summary = "Deny verification request")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Verification request denied"),
        ApiResponse(responseCode = "400", description = "Request not found or already processed"),
    )
    @PostMapping("/deny/{requestId}")
    fun denyRequest(
        @RequestHeader("User-Agent", required = false) userAgent: String?,
        @PathVariable requestId: Long,
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<*>

    @Operation(summary = "Create verification request")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Verification request created successfully"),
        ApiResponse(responseCode = "500", description = "Internal error"),
    )
    @PostMapping("/request")
    fun createVerificationRequest(
        @Valid @RequestBody dto: CreateVerificationRequestDto,
    ): ResponseEntity<*>

    @Operation(summary = "Approve verification request")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Verification request approved"),
        ApiResponse(responseCode = "400", description = "Request not found or already processed"),
        ApiResponse(responseCode = "403", description = "Unauthorized access"),
    )
    @PostMapping("/approve/{requestId}")
    fun approveRequest(
        @RequestHeader("User-Agent", required = false) userAgent: String?,
        @PathVariable requestId: Long,
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<*>
}
