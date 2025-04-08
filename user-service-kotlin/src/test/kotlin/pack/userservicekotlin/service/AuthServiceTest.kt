package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import pack.userservicekotlin.arrow.AuthServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.entities.AuthToken
import pack.userservicekotlin.domain.entities.Role
import pack.userservicekotlin.repository.*
import pack.userservicekotlin.utils.JwtTokenUtil
import java.time.Instant
import java.util.*

// iskreno mnogo je ruzno sto se hardcoduje enkodiran password
@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock lateinit var passwordEncoder: PasswordEncoder

    @Mock lateinit var jwtTokenUtil: JwtTokenUtil

    @Mock lateinit var clientRepository: ClientRepository

    @Mock lateinit var employeeRepository: EmployeeRepository

    @Mock lateinit var authTokenRepository: AuthTokenRepository

    @Mock lateinit var rabbitTemplate: RabbitTemplate

    @Mock lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var authService: AuthService

    lateinit var role: Role

    @BeforeEach
    fun setup() {
        role = Role(id = 1L, name = "CLIENT")
    }

    @Test
    fun `authenticateClient returns token if valid credentials`() {
        val client = TestDataFactory.client()
        client.email = "test@example.com"
        client.password = "hashed"
        `when`(clientRepository.findByEmail("test@example.com")).thenReturn(Optional.of(client))
        `when`(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        `when`(jwtTokenUtil.generateToken(client.email, client.id, client.role!!.name)).thenReturn("token")

        val result = authService.authenticateClient("test@example.com", "password")

        assertTrue(result.isRight())
        assertEquals("token", result.getOrNull())
    }

    @Test
    fun `authenticateClient fails if password is wrong`() {
        val client = TestDataFactory.client()
        client.email = "test@example.com"
        client.password = "hashed"
        `when`(clientRepository.findByEmail("test@example.com")).thenReturn(Optional.of(client))
        `when`(passwordEncoder.matches("wrong", "hashed")).thenReturn(false)

        val result = authService.authenticateClient("test@example.com", "wrong")

        assertTrue(result.isLeft())
        assertEquals(AuthServiceError.InvalidCredentials, result.swap().getOrNull())
    }

    @Test
    fun `authenticateEmployee returns token if valid`() {
        val employee = TestDataFactory.employee()
        employee.email = "emp@example.com"
        employee.password = "hashed"
        `when`(employeeRepository.findByEmail("emp@example.com")).thenReturn(Optional.of(employee))
        `when`(passwordEncoder.matches("password", "hashed")).thenReturn(true)
        `when`(jwtTokenUtil.generateToken(employee.email, employee.id, employee.role!!.name)).thenReturn("emp-token")

        val result = authService.authenticateEmployee("emp@example.com", "password")

        assertTrue(result.isRight())
        assertEquals("emp-token", result.getOrNull())
    }

    @Test
    fun `requestPasswordReset sends token and saves AuthToken`() {
        val client = TestDataFactory.client()
        `when`(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(client))

        val result = authService.requestPasswordReset("reset@example.com")

        assertTrue(result.isRight())
        verify(rabbitTemplate).convertAndSend(eq("reset-password"), any<EmailRequestDto>())
        verify(authTokenRepository).save(any<AuthToken>())
    }

    @Test
    fun `resetPassword updates password if token is valid`() {
        val client = TestDataFactory.client()
        val token =
            AuthToken(
                token = "abc",
                expiresAt = Instant.now().plusSeconds(60).toEpochMilli(),
                userId = 20L,
            )

        `when`(authTokenRepository.findByToken("abc")).thenReturn(Optional.of(token))
        `when`(userRepository.findById(20L)).thenReturn(Optional.of(client))
        `when`(passwordEncoder.encode("new")).thenReturn("newHashed")

        val result = authService.resetPassword("abc", "new")

        assertTrue(result.isRight())
        assertEquals("newHashed", client.password)
    }

    @Test
    fun `checkToken returns Unit if token is valid`() {
        val token = AuthToken(token = "xyz", expiresAt = Instant.now().plusSeconds(60).toEpochMilli())

        `when`(authTokenRepository.findByToken("xyz")).thenReturn(Optional.of(token))

        val result = authService.checkToken("xyz")

        assertTrue(result.isRight())
    }

    @Test
    fun `setPassword encodes and saves password`() {
        val client = TestDataFactory.client()
        val token =
            AuthToken(
                token = "setpass",
                expiresAt = Instant.now().plusSeconds(60).toEpochMilli(),
                userId = 21L,
            )

        `when`(authTokenRepository.findByToken("setpass")).thenReturn(Optional.of(token))
        `when`(userRepository.findById(21L)).thenReturn(Optional.of(client))
        `when`(passwordEncoder.encode("mypassword")).thenReturn("encoded")

        val result = authService.setPassword("setpass", "mypassword")

        assertTrue(result.isRight())
        assertEquals("encoded", client.password)
        verify(userRepository).save(client)
    }
}
