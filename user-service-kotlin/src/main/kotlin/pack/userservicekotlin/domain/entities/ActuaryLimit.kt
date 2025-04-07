package pack.userservicekotlin.domain.entities

import jakarta.persistence.*
import java.math.BigDecimal

@Entity(name = "actuary_limits")
open class ActuaryLimit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var limitAmount: BigDecimal? = null,
    open var usedLimit: BigDecimal? = null,
    open var needsApproval: Boolean = false,
    @OneToOne
    open var employee: Employee? = null,
)
