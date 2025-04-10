package pack.userservicekotlin.domain.dto.authorized_presonnel

import java.time.LocalDate

data class AuthorizedPersonnelResponseDto(
    var id: Long? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var dateOfBirth: LocalDate? = null,
    var gender: String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var address: String? = null,
    var companyId: Long? = null,
)
