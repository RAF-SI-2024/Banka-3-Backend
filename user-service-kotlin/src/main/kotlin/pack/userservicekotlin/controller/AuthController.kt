package pack.userservicekotlin.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.AuthServiceError
import pack.userservicekotlin.domain.dto.activity_code.ActivationRequestDto
import pack.userservicekotlin.domain.dto.activity_code.RequestPasswordResetDto
import pack.userservicekotlin.domain.dto.external.CheckTokenDto
import pack.userservicekotlin.domain.dto.login.LoginRequestDto
import pack.userservicekotlin.domain.dto.login.LoginResponseDto
import pack.userservicekotlin.service.AuthService
import pack.userservicekotlin.swagger.AuthApiDoc

@Tag(name = "Authentication Controller", description = "API for authenticating users")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) : AuthApiDoc {
    @PostMapping("/login/client")
    override fun clientLogin(
        @RequestBody request: LoginRequestDto,
    ): ResponseEntity<Any> =
        authService.authenticateClient(request.email, request.password).fold(
            ifLeft = {
                when (it) {
                    AuthServiceError.InvalidCredentials -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { token ->
                val response = LoginResponseDto().apply { this.token = token }
                ResponseEntity.ok(response)
            },
        )

    @PostMapping("/login/employee")
    override fun employeeLogin(
        @RequestBody request: LoginRequestDto,
    ): ResponseEntity<Any> =
        authService.authenticateEmployee(request.email, request.password).fold(
            ifLeft = {
                when (it) {
                    AuthServiceError.InvalidCredentials -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { token ->
                val response = LoginResponseDto().apply { this.token = token }
                ResponseEntity.ok(response)
            },
        )

    @PostMapping("/api/auth/request-password-reset")
    override fun requestPasswordReset(
        @RequestBody requestPasswordResetDTO: RequestPasswordResetDto,
    ): ResponseEntity<Void> =
        authService.requestPasswordReset(requestPasswordResetDTO.email).fold(
            ifLeft = {
                when (it) {
                    AuthServiceError.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PostMapping("/api/auth/check-token")
    override fun checkToken(
        @RequestBody checkTokenDTO: CheckTokenDto,
    ): ResponseEntity<Void> =
        authService.checkToken(checkTokenDTO.token).fold(
            ifLeft = {
                when (it) {
                    AuthServiceError.InvalidToken, AuthServiceError.ExpiredToken -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @PostMapping("/api/auth/set-password")
    override fun activateUser(
        @RequestBody activationRequestDto: ActivationRequestDto,
    ): ResponseEntity<Void> =
        authService.setPassword(activationRequestDto.token, activationRequestDto.password).fold(
            ifLeft = {
                when (it) {
                    AuthServiceError.InvalidToken, AuthServiceError.ExpiredToken, AuthServiceError.UserNotFound ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )
}
