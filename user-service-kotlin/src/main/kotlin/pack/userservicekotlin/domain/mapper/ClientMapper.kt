package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.domain.entities.Client

fun Client?.toDto(): ClientResponseDto? {
    if (this == null) return null
    return ClientResponseDto(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        address = address,
        phone = phone,
        gender = gender,
        birthDate = birthDate,
        jmbg = jmbg,
        username = username,
    )
}

fun CreateClientDto?.toEntity(): Client? {
    if (this == null) return null
    val client = Client()
    client.firstName = firstName
    client.lastName = lastName
    client.email = email
    client.address = address
    client.phone = phone
    client.gender = gender
    client.birthDate = birthDate
    client.password = "" // empty password on creation
    client.username = username
    client.jmbg = jmbg
    return client
}

fun UpdateClientDto?.applyTo(client: Client) {
    if (this == null) return
    client.lastName = lastName
    client.address = address
    client.phone = phone
    client.gender = gender
}
