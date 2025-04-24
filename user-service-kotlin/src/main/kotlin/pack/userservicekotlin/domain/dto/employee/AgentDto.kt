package pack.userservicekotlin.domain.dto.employee

import java.math.BigDecimal
import java.util.*

class AgentDto(
    var id: Long?,
    val username: String?,
    val position: String?,
    val department: String?,
    val active: Boolean?,
    // from BaseUser
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val jmbg: String?,
    val birthDate: Date?,
    val gender: String?,
    val phone: String?,
    val address: String?,
    val role: String?,
    val limitAmount: BigDecimal,
    val usedLimit: BigDecimal,
    val needsApproval: Boolean,
)
