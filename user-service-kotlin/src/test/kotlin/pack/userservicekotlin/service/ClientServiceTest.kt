package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import pack.userservicekotlin.arrow.ClientServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.entities.AuthToken
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.AuthTokenRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.RoleRepository
import java.util.*

@ExtendWith(MockitoExtension::class)
class ClientServiceTest {
    @Mock lateinit var clientRepository: ClientRepository

    @Mock lateinit var authTokenRepository: AuthTokenRepository

    @Mock lateinit var rabbitTemplate: RabbitTemplate

    @Mock lateinit var roleRepository: RoleRepository

    @Mock lateinit var pageable: Pageable

    @InjectMocks
    lateinit var clientService: ClientService

    @Test
    fun `listClients returns client DTOs`() {
        val clients = listOf(TestDataFactory.client(id = 1L))
        `when`(clientRepository.findAll(pageable)).thenReturn(PageImpl(clients))

        val result = clientService.listClients(pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `getClientById returns DTO if found`() {
        val client = TestDataFactory.client(id = 2L)
        `when`(clientRepository.findById(2L)).thenReturn(Optional.of(client))

        val result = clientService.getClientById(2L)

        assertTrue(result.isRight())
        assertEquals(client.id, result.getOrNull()?.id)
    }

    @Test
    fun `getClientById returns error if not found`() {
        `when`(clientRepository.findById(99L)).thenReturn(Optional.empty())

        val result = clientService.getClientById(99L)

        assertTrue(result.isLeft())
        assertEquals(ClientServiceError.NotFound(99L), result.swap().orNull())
    }

    @Test
    fun `addClient creates and saves client and token`() {
        val dto =
            CreateClientDto(
                firstName = "Anna",
                lastName = "Smith",
                email = "anna@example.com",
                gender = "F",
                phone = "5555",
                address = "Street 5",
                jmbg = "555",
                birthDate = Date(),
            )
        val role = TestDataFactory.role("CLIENT")
        val client = dto.toEntity()!!
        client.role = role

        `when`(roleRepository.findByName("CLIENT")).thenReturn(Optional.of(role))
        `when`(clientRepository.save(any())).thenReturn(client)

        val result = clientService.addClient(dto)

        assertTrue(result.isRight())
        verify(rabbitTemplate).convertAndSend(eq("set-password"), any<EmailRequestDto>())
        verify(authTokenRepository).save(any<AuthToken>())
    }

    @Test
    fun `updateClient modifies existing client`() {
        val client = TestDataFactory.client(id = 3L)
        val dto =
            UpdateClientDto(
                lastName = "Updated",
                phone = "9999",
                address = "Updated St",
                gender = "M",
            )
        `when`(clientRepository.findById(3L)).thenReturn(Optional.of(client))
        `when`(clientRepository.save(client)).thenReturn(client)

        val result = clientService.updateClient(3L, dto)

        assertTrue(result.isRight())
        assertEquals("Updated", result.getOrNull()?.lastName)
    }

    @Test
    fun `listClientsWithFilters returns filtered clients`() {
        val clients = listOf(TestDataFactory.client(id = 4L))
        `when`(clientRepository.findAll(any(), eq(pageable))).thenReturn(PageImpl(clients))

        val result = clientService.listClientsWithFilters("A", "B", "c@example.com", pageable)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `deleteClient succeeds when exists`() {
        `when`(clientRepository.existsById(5L)).thenReturn(true)

        val result = clientService.deleteClient(5L)

        assertTrue(result.isRight())
        verify(clientRepository).deleteById(5L)
    }

    @Test
    fun `deleteClient fails when not found`() {
        `when`(clientRepository.existsById(6L)).thenReturn(false)

        val result = clientService.deleteClient(6L)

        assertTrue(result.isLeft())
        assertEquals(ClientServiceError.NotFound(6L), result.swap().orNull())
    }

    @Test
    fun `findByEmail returns DTO if found`() {
        val client = TestDataFactory.client(id = 7L)
        `when`(clientRepository.findByEmail(client.email!!)).thenReturn(Optional.of(client))

        val result = clientService.findByEmail(client.email!!)

        assertTrue(result.isRight())
        assertEquals(client.email, result.getOrNull()?.email)
    }

    @Test
    fun `findByEmail returns error if not found`() {
        val email = "unknown@example.com"
        `when`(clientRepository.findByEmail(email)).thenReturn(Optional.empty())

        val result = clientService.findByEmail(email)

        assertTrue(result.isLeft())
        assertEquals(ClientServiceError.EmailNotFound(email), result.swap().orNull())
    }

    @Test
    fun `getCurrentClient fetches from SecurityContext`() {
        val email = "secure@example.com"
        val auth: Authentication = mock(Authentication::class.java)
        val context: SecurityContext = mock(SecurityContext::class.java)
        val client = TestDataFactory.client(email = email)

        `when`(auth.name).thenReturn(email)
        `when`(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        `when`(clientRepository.findByEmail(email)).thenReturn(Optional.of(client))

        val result = clientService.getCurrentClient()

        assertTrue(result.isRight())
        assertEquals(email, result.getOrNull()?.email)
    }
}
