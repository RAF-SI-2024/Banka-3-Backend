package pack.userservicekotlin.domain.dto.external

import java.util.*

data class UserResponseDto(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var address: String? = null,
    var phone: String? = null,
    var gender: String? = null,
    var birthDate: Date? = null,
    var jmbg: String? = null,
)
