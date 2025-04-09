package pack.userservicekotlin.domain.dto.verification

import pack.userservicekotlin.domain.enums.VerificationStatus
import pack.userservicekotlin.domain.enums.VerificationType
import java.time.LocalDateTime

data class VerificationResponseDto(
    val userId: Long? = null,
    val expirationTime: LocalDateTime? = null,
    val targetId: Long? = null,
    val status: VerificationStatus? = null,
    val verificationType: VerificationType? = null,
    val createdAt: LocalDateTime? = null,
)
