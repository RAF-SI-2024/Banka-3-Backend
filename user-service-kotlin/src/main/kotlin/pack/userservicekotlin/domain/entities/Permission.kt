package pack.userservicekotlin.domain.entities

import jakarta.persistence.*

@Entity
open class Permission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    @Column(unique = true, nullable = false)
    open var name: String? = null,
)
