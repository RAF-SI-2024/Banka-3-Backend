package pack.userservicekotlin.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import pack.userservicekotlin.domain.enums.VerificationStatus
import pack.userservicekotlin.domain.enums.VerificationType
import java.time.LocalDateTime

@Entity
open class VerificationRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var userId: Long? = null,
    open var targetId: Long? = null,
    @Enumerated(EnumType.STRING)
    open var status: VerificationStatus? = null,
    @Enumerated(EnumType.STRING)
    open var verificationType: VerificationType? = null,
    open var expirationTime: LocalDateTime? = null,
    @CreationTimestamp
    open var createdAt: LocalDateTime? = null,
    open var details: String? = null,
)
