package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.VerificationServiceError
import pack.userservicekotlin.domain.dto.verification.CreateVerificationRequestDto
import pack.userservicekotlin.domain.entities.VerificationRequest
import pack.userservicekotlin.domain.enums.VerificationStatus
import pack.userservicekotlin.domain.enums.VerificationType
import pack.userservicekotlin.external.BankClient
import pack.userservicekotlin.repository.VerificationRequestRepository
import pack.userservicekotlin.utils.JwtTokenUtil
import java.time.LocalDateTime

@Service
class VerificationRequestService(
    private val verificationRequestRepository: VerificationRequestRepository,
    private val bankClient: BankClient,
    private val jwtTokenUtil: JwtTokenUtil,
) {
    fun createVerificationRequest(dto: CreateVerificationRequestDto): Either<VerificationServiceError, Unit> {
        val request =
            VerificationRequest(
                userId = dto.userId,
                targetId = dto.targetId,
                status = VerificationStatus.PENDING,
                verificationType = dto.verificationType,
                expirationTime = LocalDateTime.now().plusMinutes(5),
                details = dto.details,
            )

        return try {
            verificationRequestRepository.save(request)
            Unit.right()
        } catch (e: Exception) {
            VerificationServiceError.DatabaseError("Failed to save verification request: ${e.message}").left()
        }
    }

    fun getActiveRequests(userId: Long): Either<VerificationServiceError, List<VerificationRequest>> =
        try {
            verificationRequestRepository.findActiveRequests(userId).right()
        } catch (e: Exception) {
            VerificationServiceError.DatabaseError("Failed to retrieve active requests: ${e.message}").left()
        }

    fun getRequestHistory(userId: Long): Either<VerificationServiceError, List<VerificationRequest>> =
        try {
            verificationRequestRepository.findInactiveRequests(userId).right()
        } catch (e: Exception) {
            VerificationServiceError.DatabaseError("Failed to retrieve request history: ${e.message}").left()
        }

    fun updateRequestStatus(
        requestId: Long,
        status: VerificationStatus,
    ): Either<VerificationServiceError, Boolean> {
        val request =
            verificationRequestRepository.findById(requestId).orElse(null)
                ?: return VerificationServiceError.RequestNotFound(requestId).left()

        return try {
            request.status = status
            verificationRequestRepository.save(request)
            true.right()
        } catch (e: Exception) {
            VerificationServiceError.DatabaseError("Failed to update request status: ${e.message}").left()
        }
    }

    fun processApproval(
        requestId: Long,
        authHeader: String,
    ): Either<VerificationServiceError, Boolean> {
        val clientIdFromToken = jwtTokenUtil.getUserIdFromAuthHeader(authHeader)

        val request =
            verificationRequestRepository.findActiveRequest(requestId, clientIdFromToken)
                ?: return VerificationServiceError.RequestNotFound(requestId).left()

        request.status = VerificationStatus.APPROVED
        verificationRequestRepository.save(request)

        return try {
            when (request.verificationType) {
                VerificationType.CHANGE_LIMIT -> {
                    bankClient.changeAccountLimit(request.targetId)
                    true.right()
                }
                VerificationType.PAYMENT -> {
                    bankClient.confirmPayment(request.targetId)
                    true.right()
                }
                VerificationType.TRANSFER -> {
                    bankClient.confirmTransfer(request.targetId)
                    true.right()
                }
                VerificationType.CARD_REQUEST -> {
                    bankClient.approveCardRequest(request.targetId)
                    true.right()
                }
                VerificationType.LOGIN -> TODO()
                VerificationType.LOAN -> TODO()
                null -> TODO()
            }
        } catch (e: Exception) {
            VerificationServiceError.BankServiceError("Bank service error: ${e.message}").left()
        }
    }

    fun denyVerificationRequest(
        requestId: Long,
        authHeader: String,
    ): Either<VerificationServiceError, Unit> {
        val clientIdFromToken = jwtTokenUtil.getUserIdFromAuthHeader(authHeader)

        val request =
            verificationRequestRepository.findActiveRequest(requestId, clientIdFromToken)
                ?: return VerificationServiceError.RequestNotFound(requestId).left()

        if (request.status != VerificationStatus.PENDING) {
            return VerificationServiceError.InvalidRequestStatus("Cannot deny a non-pending request").left()
        }

        request.status = VerificationStatus.DENIED
        verificationRequestRepository.save(request)

        return try {
            when (request.verificationType) {
                VerificationType.CHANGE_LIMIT -> {
                    bankClient.rejectChangeAccountLimit(request.targetId)
                    Unit.right()
                }
                VerificationType.PAYMENT -> {
                    bankClient.rejectConfirmPayment(request.targetId)
                    Unit.right()
                }
                VerificationType.TRANSFER -> {
                    bankClient.rejectConfirmTransfer(request.targetId)
                    Unit.right()
                }
                VerificationType.CARD_REQUEST -> {
                    bankClient.rejectApproveCardRequest(request.targetId)
                    Unit.right()
                }
                VerificationType.LOGIN -> TODO()
                VerificationType.LOAN -> TODO()
                null -> TODO()
            }
        } catch (e: Exception) {
            VerificationServiceError.BankServiceError("Bank service error: ${e.message}").left()
        }
    }

    fun calledFromMobile(userAgent: String?): Boolean = userAgent == "MobileApp/1.0"
}
