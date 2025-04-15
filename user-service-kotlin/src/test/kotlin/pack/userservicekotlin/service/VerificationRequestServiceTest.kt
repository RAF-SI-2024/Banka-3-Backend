package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import pack.userservicekotlin.arrow.VerificationServiceError
import pack.userservicekotlin.domain.dto.verification.CreateVerificationRequestDto
import pack.userservicekotlin.domain.entities.VerificationRequest
import pack.userservicekotlin.domain.enums.VerificationStatus
import pack.userservicekotlin.domain.enums.VerificationType
import pack.userservicekotlin.external.BankClient
import pack.userservicekotlin.repository.VerificationRequestRepository
import pack.userservicekotlin.utils.JwtTokenUtil
import java.util.*

@ExtendWith(MockitoExtension::class)
class VerificationRequestServiceTest {
    @Mock lateinit var verificationRequestRepository: VerificationRequestRepository

    @Mock lateinit var bankClient: BankClient

    @Mock lateinit var jwtTokenUtil: JwtTokenUtil

    @InjectMocks
    lateinit var service: VerificationRequestService

    @Test
    fun `createVerificationRequest saves successfully`() {
        val dto = CreateVerificationRequestDto(1L, 2L, VerificationType.PAYMENT, "some details")
        val result = service.createVerificationRequest(dto)
        assertTrue(result.isRight())
    }

    @Test
    fun `getActiveRequests returns list`() {
        val requests = listOf(mock(VerificationRequest::class.java))
        `when`(verificationRequestRepository.findActiveRequests(1L)).thenReturn(requests)

        val result = service.getActiveRequests(1L)

        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getRequestHistory returns list`() {
        val requests = listOf(mock(VerificationRequest::class.java))
        `when`(verificationRequestRepository.findInactiveRequests(1L)).thenReturn(requests)

        val result = service.getRequestHistory(1L)

        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `updateRequestStatus updates status when found`() {
        val request = VerificationRequest(status = VerificationStatus.PENDING)
        `when`(verificationRequestRepository.findById(10L)).thenReturn(Optional.of(request))

        val result = service.updateRequestStatus(10L, VerificationStatus.APPROVED)

        assertTrue(result.isRight())
        assertEquals(VerificationStatus.APPROVED, request.status)
    }

    @Test
    fun `updateRequestStatus returns error if not found`() {
        `when`(verificationRequestRepository.findById(99L)).thenReturn(Optional.empty())

        val result = service.updateRequestStatus(99L, VerificationStatus.APPROVED)

        assertTrue(result.isLeft())
        assertTrue(result.swap().orNull() is VerificationServiceError.RequestNotFound)
    }

    @Test
    fun `processApproval approves request and calls bank client`() {
        val request =
            VerificationRequest(
                status = VerificationStatus.PENDING,
                verificationType = VerificationType.PAYMENT,
                targetId = 22L,
            )
        `when`(jwtTokenUtil.getUserIdFromAuthHeader("token")).thenReturn(1L)
        `when`(verificationRequestRepository.findActiveRequest(10L, 1L)).thenReturn(request)

        val result = service.processApproval(10L, "token")

        assertTrue(result.isRight())
        assertEquals(VerificationStatus.APPROVED, request.status)
        verify(bankClient).confirmPayment(22L)
    }

    @Test
    fun `denyVerificationRequest denies pending request`() {
        val request =
            VerificationRequest(
                status = VerificationStatus.PENDING,
                verificationType = VerificationType.CARD_REQUEST,
                targetId = 101L,
            )
        `when`(jwtTokenUtil.getUserIdFromAuthHeader("token")).thenReturn(2L)
        `when`(verificationRequestRepository.findActiveRequest(20L, 2L)).thenReturn(request)

        val result = service.denyVerificationRequest(20L, "token")

        assertTrue(result.isRight())
        assertEquals(VerificationStatus.DENIED, request.status)
        verify(bankClient).rejectApproveCardRequest(101L)
    }

    @Test
    fun `denyVerificationRequest fails if status is not PENDING`() {
        val request =
            VerificationRequest(
                status = VerificationStatus.APPROVED,
                verificationType = VerificationType.TRANSFER,
                targetId = 101L,
            )
        `when`(jwtTokenUtil.getUserIdFromAuthHeader("token")).thenReturn(2L)
        `when`(verificationRequestRepository.findActiveRequest(20L, 2L)).thenReturn(request)

        val result = service.denyVerificationRequest(20L, "token")

        assertTrue(result.isLeft())
        assertTrue(result.swap().orNull() is VerificationServiceError.InvalidRequestStatus)
    }

    @Test
    fun `calledFromMobile returns true for matching agent`() {
        val result = service.calledFromMobile("MobileApp/1.0")
        assertTrue(result)
    }

    @Test
    fun `calledFromMobile returns false for anything else`() {
        val result = service.calledFromMobile("Browser/99")
        assertFalse(result)
    }
}
