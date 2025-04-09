package pack.userservicekotlin.domain.entities

import jakarta.persistence.*
import java.util.*

@Entity(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
open class BaseUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    open var id: Long? = null,
    @Column(updatable = false, unique = true)
    open var username: String? = null,
    @Column(updatable = false)
    open var firstName: String? = null,
    open var lastName: String? = null,
    @Column(updatable = false)
    @Temporal(TemporalType.DATE)
    open var birthDate: Date? = null,
    open var gender: String? = null,
    @Column(updatable = false, unique = true)
    open var email: String? = null,
    open var phone: String? = null,
    open var address: String? = null,
    open var password: String? = null,
    @Column(updatable = false, unique = true)
    open var jmbg: String? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    open var role: Role? = null,
)
