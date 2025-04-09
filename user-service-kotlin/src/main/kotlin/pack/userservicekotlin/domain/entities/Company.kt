package pack.userservicekotlin.domain.entities

import jakarta.persistence.*

@Entity
open class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var name: String? = null,
    @Column(updatable = false, unique = true)
    open var registrationNumber: String? = null,
    @Column(updatable = false, unique = true)
    open var taxId: String? = null,
    open var activityCode: String? = null,
    open var address: String? = null,
    @ManyToOne
    open var majorityOwner: Client? = null,
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    open var authorizedPersonel: MutableList<AuthorizedPersonel> = mutableListOf(),
)
