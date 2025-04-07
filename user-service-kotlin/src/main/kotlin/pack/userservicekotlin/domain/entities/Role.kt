package pack.userservicekotlin.domain.entities

import jakarta.persistence.*

@Entity
@Table(name = "roles")
open class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    @Column(nullable = false, unique = true)
    open var name: String? = null,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    open var permissions: MutableSet<Permission> = mutableSetOf(),
)
