package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pack.userservicekotlin.arrow.AuthServiceError
import pack.userservicekotlin.domain.dto.activity_code.ActivationRequestDto
import pack.userservicekotlin.domain.dto.activity_code.RequestPasswordResetDto
import pack.userservicekotlin.domain.dto.external.CheckTokenDto
import pack.userservicekotlin.domain.dto.login.LoginRequestDto
import pack.userservicekotlin.service.AuthService
import pack.userservicekotlin.utils.JwtTokenUtil

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var authService: AuthService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @Test
    fun `clientLogin should return token when credentials are valid`() {
        val loginRequest = LoginRequestDto(email = "client@example.com", password = "password123")
        val token = "mocked-jwt-token"

        `when`(authService.authenticateClient(loginRequest.email, loginRequest.password))
            .thenReturn(Either.Right(token))

        mockMvc
            .post("/api/auth/login/client") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isOk() }
                jsonPath("$.token") { value(token) }
            }
    }

    @Test
    fun `clientLogin should return 401 when credentials are invalid`() {
        val loginRequest = LoginRequestDto(email = "client@example.com", password = "wrongpassword")

        `when`(authService.authenticateClient(loginRequest.email, loginRequest.password))
            .thenReturn(Either.Left(AuthServiceError.InvalidCredentials))

        mockMvc
            .post("/api/auth/login/client") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isUnauthorized() }
                content { string("Bad credentials") }
            }
    }

    @Test
    fun `employeeLogin should return token when credentials are valid`() {
        val loginRequest = LoginRequestDto(email = "employee@example.com", password = "password123")
        val token = "mocked-jwt-token"

        `when`(authService.authenticateEmployee(loginRequest.email, loginRequest.password))
            .thenReturn(Either.Right(token))

        mockMvc
            .post("/api/auth/login/employee") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isOk() }
                jsonPath("$.token") { value(token) }
            }
    }

    @Test
    fun `employeeLogin should return 401 when credentials are invalid`() {
        val loginRequest = LoginRequestDto(email = "employee@example.com", password = "wrongpassword")

        `when`(authService.authenticateEmployee(loginRequest.email, loginRequest.password))
            .thenReturn(Either.Left(AuthServiceError.InvalidCredentials))

        mockMvc
            .post("/api/auth/login/employee") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(loginRequest)
            }.andExpect {
                status { isUnauthorized() }
                content { string("Bad credentials") }
            }
    }

    /** Calling external services */

//    @Test
//    fun `requestPasswordReset should return 200 when email exists`() {
//        val request = RequestPasswordResetDto(email = "user@example.com")
//
//        `when`(authService.requestPasswordReset(request.email))
//            .thenReturn(Either.Right(Unit))
//
//        mockMvc
//            .post("/api/auth/request-password-reset") {
//                contentType = MediaType.APPLICATION_JSON
//                content = objectMapper.writeValueAsString(request)
//            }.andExpect {
//                status { isOk() }
//            }
//    }

    @Test
    fun `requestPasswordReset should return 404 when email does not exist`() {
        val request = RequestPasswordResetDto(email = "nonexistent@example.com")

        `when`(authService.requestPasswordReset(request.email))
            .thenReturn(Either.Left(AuthServiceError.UserNotFound))

        mockMvc
            .post("/api/auth/request-password-reset") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isNotFound() }
            }
    }

    /** Calling external services */

//    @Test
//    fun `checkToken should return 200 when token is valid`() {
//        val request = CheckTokenDto(token = "valid-token")
//
//        `when`(authService.checkToken(request.token))
//            .thenReturn(Either.Right(Unit))
//
//        mockMvc
//            .post("/api/auth/check-token") {
//                contentType = MediaType.APPLICATION_JSON
//                content = objectMapper.writeValueAsString(request)
//            }.andExpect {
//                status { isOk() }
//            }
//    }

    @Test
    fun `checkToken should return 404 when token is invalid`() {
        val request = CheckTokenDto(token = "invalid-token")

        `when`(authService.checkToken(request.token))
            .thenReturn(Either.Left(AuthServiceError.InvalidToken))

        mockMvc
            .post("/api/auth/check-token") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isNotFound() }
            }
    }

    /** Calling external services */

//    @Test
//    fun `activateUser should return 200 when token and password are valid`() {
//        val request = ActivationRequestDto(token = "valid-token", password = "newpassword123")
//
//        `when`(authService.setPassword(request.token, request.password))
//            .thenReturn(Either.Right(Unit))
//
//        mockMvc
//            .post("/api/auth/set-password") {
//                contentType = MediaType.APPLICATION_JSON
//                content = objectMapper.writeValueAsString(request)
//            }.andExpect {
//                status { isOk() }
//            }
//    }

    @Test
    fun `activateUser should return 404 when token is invalid`() {
        val request = ActivationRequestDto(token = "invalid-token", password = "newpassword123")

        `when`(authService.setPassword(request.token, request.password))
            .thenReturn(Either.Left(AuthServiceError.InvalidToken))

        mockMvc
            .post("/api/auth/set-password") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isNotFound() }
            }
    }
}
