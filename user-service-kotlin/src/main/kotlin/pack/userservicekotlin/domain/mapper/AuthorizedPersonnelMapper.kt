package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.authorized_presonnel.AuthorizedPersonnelResponseDto
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.domain.entities.AuthorizedPersonel
import pack.userservicekotlin.domain.entities.Company

fun AuthorizedPersonel?.toDto(): AuthorizedPersonnelResponseDto? {
    if (this == null) return null

    return AuthorizedPersonnelResponseDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        email = email,
        phoneNumber = phoneNumber,
        address = address,
        companyId = company?.id,
    )
}

fun CreateAuthorizedPersonnelDto?.toEntity(company: Company): AuthorizedPersonel? {
    if (this == null) return null

    return AuthorizedPersonel(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        email = email,
        phoneNumber = phoneNumber,
        address = address,
        company = company,
    )
}
