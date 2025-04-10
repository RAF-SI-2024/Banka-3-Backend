package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.external.UserResponseDto
import pack.userservicekotlin.domain.entities.BaseUser

fun BaseUser?.toDto(): UserResponseDto? {
    if (this == null) return null

    val dto = UserResponseDto()
    dto.id = id
    dto.firstName = firstName
    dto.lastName = lastName
    dto.email = email
    dto.address = address
    dto.phone = phone
    dto.gender = gender
    dto.birthDate = birthDate
    dto.jmbg = jmbg
    return dto
}
