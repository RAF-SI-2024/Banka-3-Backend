package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.AuthServiceError
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.entities.AuthToken
import pack.userservicekotlin.domain.entities.Client
import pack.userservicekotlin.domain.entities.Employee
import pack.userservicekotlin.repository.AuthTokenRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.EmployeeRepository
import pack.userservicekotlin.repository.UserRepository
import pack.userservicekotlin.utils.JwtTokenUtil
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil,
    private val clientRepository: ClientRepository,
    private val employeeRepository: EmployeeRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val userRepository: UserRepository,
) {
    fun authenticateClient(
        email: String?,
        password: String?,
    ): Either<AuthServiceError, String> {
        val user =
            clientRepository.findByEmail(email!!).orElse(null)
                ?: return AuthServiceError.InvalidCredentials.left()

        return if (!passwordEncoder.matches(password, user.password)) {
            AuthServiceError.InvalidCredentials.left()
        } else {
            jwtTokenUtil.generateToken(user.email, user.id, user.role?.name).right()
        }
    }

    fun authenticateEmployee(
        email: String?,
        password: String?,
    ): Either<AuthServiceError, String> {
        val user =
            employeeRepository.findByEmail(email!!).orElse(null)
                ?: return AuthServiceError.InvalidCredentials.left()

        return if (!passwordEncoder.matches(password, user.password)) {
            AuthServiceError.InvalidCredentials.left()
        } else {
            jwtTokenUtil.generateToken(user.email, user.id, user.role!!.name).right()
        }
    }

    fun requestPasswordReset(email: String?): Either<AuthServiceError, Unit> {
        val user =
            userRepository.findByEmail(email!!).orElse(null)
                ?: return AuthServiceError.UserNotFound.left()

        val token = UUID.randomUUID().toString()
        val emailRequestDto = EmailRequestDto(token, email)
        rabbitTemplate.convertAndSend("reset-password", emailRequestDto)

        val createdAt = Instant.now().toEpochMilli()
        val expiresAt = createdAt + 86400000

        authTokenRepository.save(
            AuthToken(
                createdAt = createdAt,
                expiresAt = expiresAt,
                token = token,
                type = "reset-password",
                userId = user.id,
            ),
        )

        return Unit.right()
    }

    fun resetPassword(
        token: String?,
        password: String?,
    ): Either<AuthServiceError, Unit> {
        val authToken =
            authTokenRepository.findByToken(token!!).orElse(null)
                ?: return AuthServiceError.InvalidToken.left()

        if (authToken.expiresAt!! <= Instant.now().toEpochMilli()) {
            return AuthServiceError.ExpiredToken.left()
        }

        val user =
            userRepository.findById(authToken.userId!!).orElse(null)
                ?: return AuthServiceError.UserNotFound.left()

        authToken.expiresAt = Instant.now().toEpochMilli()
        user.password = passwordEncoder.encode(password)

        when (user) {
            is Client -> clientRepository.save(user)
            is Employee -> employeeRepository.save(user)
        }

        return Unit.right()
    }

    fun checkToken(token: String?): Either<AuthServiceError, Unit> {
        val authToken =
            authTokenRepository.findByToken(token!!).orElse(null)
                ?: return AuthServiceError.InvalidToken.left()

        return if (authToken.expiresAt!! < Instant.now().toEpochMilli()) {
            AuthServiceError.ExpiredToken.left()
        } else {
            Unit.right()
        }
    }

    fun setPassword(
        token: String?,
        password: String?,
    ): Either<AuthServiceError, Unit> {
        val authToken =
            authTokenRepository.findByToken(token!!).orElse(null)
                ?: return AuthServiceError.InvalidToken.left()

        if (authToken.expiresAt!! <= Instant.now().toEpochMilli()) {
            return AuthServiceError.ExpiredToken.left()
        }

        val user =
            userRepository.findById(authToken.userId!!).orElse(null)
                ?: return AuthServiceError.UserNotFound.left()

        authToken.expiresAt = Instant.now().toEpochMilli()
        user.password = passwordEncoder.encode(password)
        userRepository.save(user)

        return Unit.right()
    }
}
