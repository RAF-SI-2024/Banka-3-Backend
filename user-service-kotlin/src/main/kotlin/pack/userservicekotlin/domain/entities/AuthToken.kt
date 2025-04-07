package pack.userservicekotlin.domain.entities

import jakarta.persistence.*

@Entity
open class AuthToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var createdAt: Long? = null,
    open var expiresAt: Long? = null,
    open var token: String? = null,
    open var type: String? = null,
    open var userId: Long? = null,
)
