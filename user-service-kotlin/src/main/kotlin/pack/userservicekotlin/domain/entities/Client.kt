package pack.userservicekotlin.domain.entities

import jakarta.persistence.*
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("CLT")
open class Client(
    @OneToMany(mappedBy = "majorityOwner", cascade = [CascadeType.ALL], orphanRemoval = true)
    open var companies: MutableList<Company> = mutableListOf(),
) : BaseUser()
