package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.ClientServiceError
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.entities.AuthToken
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.AuthTokenRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.RoleRepository
import pack.userservicekotlin.specification.ClientSearchSpecification
import java.time.Instant
import java.util.*

@Service
class ClientService(
    private val clientRepository: ClientRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val roleRepository: RoleRepository,
) {
    fun listClients(pageable: Pageable): Page<ClientResponseDto> = clientRepository.findAll(pageable).map { it.toDto()!! }

    fun getClientById(id: Long): Either<ClientServiceError, ClientResponseDto> =
        clientRepository
            .findById(id)
            .map { it.toDto()!! }
            .orElse(null)
            ?.right()
            ?: ClientServiceError.NotFound(id).left()

    fun addClient(dto: CreateClientDto): Either<ClientServiceError, ClientResponseDto> {
        if (clientRepository.findByEmail(dto.email!!).isPresent) {
            return ClientServiceError.EmailAlreadyExists.left()
        }
        if (clientRepository.findByJmbg(dto.jmbg!!).isPresent) {
            return ClientServiceError.JmbgAlreadyExists.left()
        }
        if (clientRepository.findByUsername(dto.username!!).isPresent) {
            return ClientServiceError.UsernameAlreadyExists.left()
        }

        val client = dto.toEntity() ?: return ClientServiceError.InvalidInput.left()
        client.password = ""

        val role =
            roleRepository.findByName("CLIENT").orElse(null)
                ?: return ClientServiceError.RoleNotFound("Unknown Role").left()

        client.role = role

        return try {
            val savedClient = clientRepository.save(client)

            val token = UUID.randomUUID()
            rabbitTemplate.convertAndSend("set-password", EmailRequestDto(token.toString(), client.email))

            val createdAt = Instant.now().toEpochMilli()
            val expiresAt = createdAt + 86_400_000 // 24h

            val authToken =
                AuthToken(
                    createdAt = createdAt,
                    expiresAt = expiresAt,
                    token = token.toString(),
                    type = "set-password",
                    userId = savedClient.id,
                )
            authTokenRepository.save(authToken)

            savedClient.toDto()!!.right()
        } catch (e: Exception) {
            e.printStackTrace()
            ClientServiceError.Unknown.left()
        }
    }

    fun updateClient(
        id: Long,
        dto: UpdateClientDto,
    ): Either<ClientServiceError, ClientResponseDto> {
        val existingClient =
            clientRepository.findById(id).orElse(null)
                ?: return ClientServiceError.NotFound(id).left()

        existingClient.apply {
            lastName = dto.lastName
            address = dto.address
            phone = dto.phone
            gender = dto.gender
        }

        val updatedClient = clientRepository.save(existingClient)
        return updatedClient.toDto()!!.right()
    }

    fun listClientsWithFilters(
        firstName: String?,
        lastName: String?,
        email: String?,
        pageable: Pageable,
    ): Page<ClientResponseDto> {
        val spec =
            Specification
                .where(ClientSearchSpecification.firstNameContains(firstName))
                .and(ClientSearchSpecification.lastNameContains(lastName))
                .and(ClientSearchSpecification.emailContains(email))

        return clientRepository.findAll(spec, pageable).map { it.toDto()!! }
    }

    fun deleteClient(id: Long): Either<ClientServiceError, Unit> =
        if (!clientRepository.existsById(id)) {
            ClientServiceError.NotFound(id).left()
        } else {
            clientRepository.deleteById(id)
            Unit.right()
        }

    fun findByEmail(email: String): Either<ClientServiceError, ClientResponseDto> =
        clientRepository
            .findByEmail(email)
            .map { it.toDto()!! }
            .orElse(null)
            ?.right()
            ?: ClientServiceError.EmailNotFound(email).left()

    fun getCurrentClient(): Either<ClientServiceError, ClientResponseDto> {
        val auth = SecurityContextHolder.getContext().authentication
            ?: return ClientServiceError.NotAuthenticated("User not authenticated").left()
        
        val email = auth.name
        return findByEmail(email)
    }
}
