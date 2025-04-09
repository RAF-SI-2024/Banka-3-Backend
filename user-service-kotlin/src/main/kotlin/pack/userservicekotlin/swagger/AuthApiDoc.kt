package pack.userservicekotlin.swagger

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.responses.*
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.domain.dto.activity_code.ActivationRequestDto
import pack.userservicekotlin.domain.dto.activity_code.RequestPasswordResetDto
import pack.userservicekotlin.domain.dto.external.CheckTokenDto
import pack.userservicekotlin.domain.dto.login.LoginRequestDto

@Tag(name = "Authentication Controller", description = "API for authenticating users")
interface AuthApiDoc {
    @Operation(summary = "Client Login", description = "Endpoint for logging client and generating JWT token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully generated JWT token"),
            ApiResponse(responseCode = "401", description = "Bad credentials"),
        ],
    )
    @PostMapping("/api/auth/login/client")
    fun clientLogin(
        @RequestBody request: LoginRequestDto,
    ): ResponseEntity<*>

    @Operation(summary = "Employee Login", description = "Endpoint for logging employee and generating JWT token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully generated JWT token"),
            ApiResponse(responseCode = "401", description = "Bad credentials"),
        ],
    )
    @PostMapping("/api/auth/login/employee")
    fun employeeLogin(
        @RequestBody request: LoginRequestDto,
    ): ResponseEntity<*>

    @Operation(summary = "Request password reset", description = "Requests password reset with email address.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Request for password reset successfully sent."),
            ApiResponse(responseCode = "400", description = "Invalid email."),
        ],
    )
    @PostMapping("/api/auth/request-password-reset")
    fun requestPasswordReset(
        @RequestBody requestPasswordResetDTO: RequestPasswordResetDto,
    ): ResponseEntity<Void>

    @Operation(summary = "Checks if a token is still valid", description = "Checks if a token is still valid.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Valid"),
            ApiResponse(responseCode = "404", description = "Invalid"),
        ],
    )
    @PostMapping("/api/auth/check-token")
    fun checkToken(
        @RequestBody checkTokenDTO: CheckTokenDto,
    ): ResponseEntity<Void>

    @Operation(summary = "Sets password", description = "Sets new password for both clients and employees")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Password set successfully."),
            ApiResponse(responseCode = "400", description = "Invalid data."),
        ],
    )
    @PostMapping("/api/auth/set-password")
    fun activateUser(
        @RequestBody activationRequestDto: ActivationRequestDto,
    ): ResponseEntity<Void>
}
