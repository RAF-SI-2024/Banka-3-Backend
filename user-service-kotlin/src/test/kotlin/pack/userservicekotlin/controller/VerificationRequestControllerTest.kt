package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import feign.FeignException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.ClientServiceError
import pack.userservicekotlin.arrow.VerificationServiceError
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.verification.CreateVerificationRequestDto
import pack.userservicekotlin.domain.entities.VerificationRequest
import pack.userservicekotlin.domain.enums.VerificationType
import pack.userservicekotlin.service.ClientService
import pack.userservicekotlin.service.VerificationRequestService
import pack.userservicekotlin.utils.JwtTokenUtil

@WebMvcTest(VerificationRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
class VerificationRequestControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var verificationRequestService: VerificationRequestService

    @MockitoBean lateinit var clientService: ClientService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    private val mobileHeader = "MobileApp/1.0"
    private val client = ClientResponseDto(id = 1L, firstName = "John", lastName = "Doe", email = "john@doe.com")

    @Test
    fun `getActiveRequests should return 200 with results`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(clientService.getCurrentClient()).thenReturn(Either.Right(client))
        `when`(verificationRequestService.getActiveRequests(client.id!!)).thenReturn(
            Either.Right(listOf(mock(VerificationRequest::class.java))),
        )

        mockMvc
            .get("/api/verification/active-requests") {
                header("User-Agent", mobileHeader)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `getRequestHistory should return 200 with results`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(clientService.getCurrentClient()).thenReturn(Either.Right(client))
        `when`(verificationRequestService.getRequestHistory(client.id!!)).thenReturn(
            Either.Right(listOf(mock(VerificationRequest::class.java))),
        )

        mockMvc
            .get("/api/verification/history") {
                header("User-Agent", mobileHeader)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `denyRequest should return 200 when successful`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(verificationRequestService.denyVerificationRequest(1L, "auth")).thenReturn(Either.Right(Unit))

        mockMvc
            .post("/api/verification/deny/1") {
                header("User-Agent", mobileHeader)
                header("Authorization", "auth")
            }.andExpect {
                status { isOk() }
                content { string("Request denied successfully.") }
            }
    }

    @Test
    fun `createVerificationRequest should return 200 when successful`() {
        val dto = CreateVerificationRequestDto(1L, 2L, VerificationType.PAYMENT, "test")
        `when`(verificationRequestService.createVerificationRequest(dto)).thenReturn(Either.Right(Unit))

        mockMvc
            .post("/api/verification/request") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isOk() }
                content { string("Verification request created.") }
            }
    }

    @Test
    fun `approveRequest should return 200 when successful`() {
        `when`(verificationRequestService.calledFromMobile("MobileApp/1.0")).thenReturn(true)
        `when`(verificationRequestService.processApproval(1L, "auth")).thenReturn(Either.Right(true))

        mockMvc
            .post("/api/verification/approve/1") {
                header("User-Agent", "MobileApp/1.0")
                header("Authorization", "auth")
            }.andExpect {
                status { isOk() }
                content { string("Request approved.") }
            }
    }

    @Test
    fun `approveRequest should return 400 if feign throws`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(verificationRequestService.processApproval(1L, "auth"))
            .thenThrow(FeignException.BadRequest::class.java)

        mockMvc
            .post("/api/verification/approve/1") {
                header("User-Agent", mobileHeader)
                header("Authorization", "auth")
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `denyRequest should return 400 when service returns error`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(verificationRequestService.denyVerificationRequest(1L, "auth"))
            .thenReturn(Either.Left(VerificationServiceError.InvalidRequestStatus("Already handled")))

        mockMvc
            .post("/api/verification/deny/1") {
                header("User-Agent", mobileHeader)
                header("Authorization", "auth")
            }.andExpect {
                status { isBadRequest() }
                content { string("InvalidRequestStatus(message=Already handled)") }
            }
    }

    @Test
    fun `getActiveRequests returns 401 if not mobile`() {
        mockMvc
            .get("/api/verification/active-requests") {
                header("User-Agent", "Chrome")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `getActiveRequests should return 404 if client not found`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(clientService.getCurrentClient()).thenReturn(Either.Left(ClientServiceError.EmailNotFound("test@example.com")))

        mockMvc
            .get("/api/verification/active-requests") {
                header("User-Agent", mobileHeader)
            }.andExpect {
                status { isNotFound() }
                content { string("Client not found: EmailNotFound(email=test@example.com)") }
            }
    }

    @Test
    fun `getRequestHistory should return 404 if client not found`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(clientService.getCurrentClient()).thenReturn(Either.Left(ClientServiceError.EmailNotFound("test@example.com")))

        mockMvc
            .get("/api/verification/history") {
                header("User-Agent", mobileHeader)
            }.andExpect {
                status { isNotFound() }
                content { string("Client not found: EmailNotFound(email=test@example.com)") }
            }
    }

    @Test
    fun `getRequestHistory returns 401 if not mobile`() {
        mockMvc
            .get("/api/verification/history") {
                header("User-Agent", "Chrome")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `createVerificationRequest should return 500 if service returns error`() {
        val dto = CreateVerificationRequestDto(1L, 2L, VerificationType.PAYMENT, "test")
        `when`(verificationRequestService.createVerificationRequest(dto))
            .thenReturn(Either.Left(VerificationServiceError.InvalidRequestStatus("Invalid request")))

        mockMvc
            .post("/api/verification/request") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isInternalServerError() }
                content { string("InvalidRequestStatus(message=Invalid request)") }
            }
    }

    @Test
    fun `createVerificationRequest should return 400 if invalid request body`() {
        mockMvc
            .post("/api/verification/request") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `approveRequest should return 400 if service returns error`() {
        `when`(verificationRequestService.calledFromMobile(mobileHeader)).thenReturn(true)
        `when`(verificationRequestService.processApproval(1L, "auth"))
            .thenReturn(Either.Left(VerificationServiceError.InvalidRequestStatus("Already approved")))

        mockMvc
            .post("/api/verification/approve/1") {
                header("User-Agent", mobileHeader)
                header("Authorization", "auth")
            }.andExpect {
                status { isBadRequest() }
                content { string("InvalidRequestStatus(message=Already approved)") }
            }
    }

    @Test
    fun `approveRequest returns 401 if not mobile`() {
        mockMvc
            .post("/api/verification/approve/1") {
                header("User-Agent", "Chrome")
                header("Authorization", "auth")
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `denyRequest returns 401 if not mobile`() {
        mockMvc
            .post("/api/verification/deny/1") {
                header("User-Agent", "Chrome")
                header("Authorization", "auth")
            }.andExpect {
                status { isUnauthorized() }
            }
    }
}
