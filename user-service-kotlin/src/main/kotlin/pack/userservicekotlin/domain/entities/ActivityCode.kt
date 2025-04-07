package pack.userservicekotlin.domain.entities

import jakarta.persistence.*

@Entity
open class ActivityCode(
    @Id
    open var id: String? = null,
    open var description: String? = null,
)
