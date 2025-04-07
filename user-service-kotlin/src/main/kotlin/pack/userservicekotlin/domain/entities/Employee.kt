package pack.userservicekotlin.domain.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.OneToOne

@Entity
@DiscriminatorValue("EMP")
open class Employee(
    open var position: String? = null,
    open var department: String? = null,
    open var active: Boolean = true,
    @OneToOne(mappedBy = "employee", cascade = [CascadeType.REMOVE])
    open var actuaryLimit: ActuaryLimit? = null,
) : BaseUser()
