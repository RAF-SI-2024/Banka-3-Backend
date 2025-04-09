package pack.userservicekotlin.domain.entities

import jakarta.persistence.*
import java.time.LocalDate

@Entity(name = "authorized_personel")
open class AuthorizedPersonel(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var firstName: String? = null,
    open var lastName: String? = null,
    open var dateOfBirth: LocalDate? = null,
    open var gender: String? = null,
    open var email: String? = null,
    open var phoneNumber: String? = null,
    open var address: String? = null,
    @ManyToOne
    @JoinColumn(name = "company_id")
    open var company: Company? = null,
)
