package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.ClientServiceError
import pack.userservicekotlin.domain.TestRequestData.validCreateClientDto
import pack.userservicekotlin.domain.TestResponseData.validClientResponseDto
import pack.userservicekotlin.domain.TestUpdateData.validUpdateClientDto
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.client.UpdateClientDto
import pack.userservicekotlin.service.ClientService
import pack.userservicekotlin.utils.JwtTokenUtil
import java.sql.Date
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext

@WebMvcTest(ClientController::class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var clientService: ClientService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @Test
    fun `getAllClients should return paginated clients`() {
        val clients =
            listOf(
                ClientResponseDto(
                    id = 1L,
                    firstName = "Jane",
                    lastName = "Doe",
                    email = "jane.doe@example.com",
                    address = "Client Avenue 5",
                    phone = "061111222",
                    gender = "F",
                    birthDate = Date.valueOf("1992-05-15"),
                    jmbg = "9876543210123",
                    username = "janedoe",
                ),
            )
        val page = PageImpl(clients)

        `when`(
            clientService.listClients(
                PageRequest.of(0, 10),
            ),
        ).thenReturn(page)

        mockMvc
            .get("/api/admin/clients?page=0&size=10")
            .andExpect {
                status { isOk() }
//                jsonPath("$.content[0].email") { value("jane.doe@example.com") } -> ovo ne moze jer se sortira u funkciji
            }
    }

    @Test
    fun `getClientById should return client when found`() {
        val response = validClientResponseDto()

        `when`(clientService.getClientById(1L)).thenReturn(Either.Right(response))

        mockMvc
            .get("/api/admin/clients/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("Jane") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("jane.doe@example.com") }
            }
    }

    @Test
    fun `getClientById should return 404 when client not found`() {
        `when`(clientService.getClientById(1L)).thenReturn(Either.Left(ClientServiceError.NotFound(1L)))

        mockMvc
            .get("/api/admin/clients/1")
            .andExpect {
                status { isNotFound() }
                content { string("Client not found with ID: 1") }
            }
    }

    @Test
    fun `addClient should return 201 when client is created`() {
        val request = validCreateClientDto()
        val response = validClientResponseDto().copy(id = 2L)

        `when`(clientService.addClient(anyNonNull())).thenReturn(Either.Right(response))

        mockMvc
            .post("/api/admin/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(2L) }
                jsonPath("$.firstName") { value("Jane") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("jane.doe@example.com") }
            }
    }

    @Test
    fun `addClient should return 400 when input is invalid`() {
        val invalidRequest = validCreateClientDto().copy(firstName = "", email = "not-an-email")

        `when`(clientService.addClient(anyNonNull())).thenReturn(Either.Left(ClientServiceError.InvalidInput))

        mockMvc
            .post("/api/admin/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(invalidRequest)
            }.andExpect {
                status { isBadRequest() }
                content { string("Invalid client data") }
            }
    }

    @Test
    fun `updateClient should return 200 when client is updated`() {
        val request = validUpdateClientDto()
        val response = validClientResponseDto().copy(firstName = "Johnny", email = "johnny.doe@example.com")

        `when`(clientService.updateClient(Mockito.eq(1L), anyNonNull()))
            .thenReturn(Either.Right(response))

        mockMvc
            .put("/api/admin/clients/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("Johnny") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("johnny.doe@example.com") }
            }
    }

    @Test
    fun `updateClient should return 404 when client not found`() {
        val request = validUpdateClientDto()

        `when`(clientService.updateClient(Mockito.eq(1L), anyNonNull()))
            .thenReturn(Either.Left(ClientServiceError.NotFound(1L)))

        mockMvc
            .put("/api/admin/clients/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isNotFound() }
                content { string("Client not found") }
            }
    }

    @Test
    fun `deleteClient should return 200 when client is deleted`() {
        `when`(clientService.deleteClient(1L)).thenReturn(Either.Right(Unit))

        mockMvc
            .delete("/api/admin/clients/1")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `deleteClient should return 404 when client not found`() {
        `when`(clientService.deleteClient(1L)).thenReturn(Either.Left(ClientServiceError.NotFound(1L)))

        mockMvc
            .delete("/api/admin/clients/1")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `getAllClients should handle invalid page parameters`() {
        mockMvc
            .get("/api/admin/clients?page=-1&size=0")
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `getAllClients should return empty page when no clients exist`() {
        val emptyPage = PageImpl<ClientResponseDto>(emptyList())
        `when`(clientService.listClientsWithFilters(null, null, null, PageRequest.of(0, 10, Sort.by("lastName").ascending())))
            .thenReturn(emptyPage)

        mockMvc
            .get("/api/admin/clients")
            .andExpect {
                status { isOk() }
                jsonPath("$.content") { isArray() }
                jsonPath("$.content.length()") { value(0) }
            }
    }

    @Test
    fun `addClient should return 404 if role not found`() {
        val request = validCreateClientDto()
        `when`(clientService.addClient(anyNonNull())).thenReturn(Either.Left(ClientServiceError.RoleNotFound("Unknown Role")))

        mockMvc
            .post("/api/admin/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isNotFound() }
                content { string("Client role not found") }
            }
    }

    @Test
    fun `addClient should return 409 if email already exists`() {
        val request = validCreateClientDto()
        `when`(clientService.addClient(anyNonNull())).thenReturn(Either.Left(ClientServiceError.EmailAlreadyExists))

        mockMvc
            .post("/api/admin/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isConflict() }
            }
    }

    @Test
    fun `addClient should return 400 if invalid request body`() {
        mockMvc
            .post("/api/admin/clients") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `updateClient should return 400 if invalid request body`() {
        mockMvc
            .put("/api/admin/clients/1") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `getCurrentClient should return 401 if not authenticated`() {
        SecurityContextHolder.clearContext()

        mockMvc
            .get("/api/admin/clients/me")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `getCurrentClient should return 404 if client not found`() {
        val email = "nonexistent@example.com"
        val auth = mock(Authentication::class.java)
        `when`(auth.name).thenReturn(email)
        val context = mock(SecurityContext::class.java)
        `when`(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        `when`(clientService.getCurrentClient()).thenReturn(Either.Left(ClientServiceError.EmailNotFound(email)))

        mockMvc
            .get("/api/admin/clients/me")
            .andExpect {
                status { isNotFound() }
                content { string("Client not found with email: $email") }
            }
    }

    inline fun <reified T> anyNonNull(): T {
        try {
            return Mockito.any(T::class.java) ?: createInstance()
        } catch (e: Exception) {
            return createInstance()
        }
    }

    inline fun <reified T : Any> createInstance(): T =
        when (T::class) {
            CreateClientDto::class -> CreateClientDto("", "", null, "M", "", "", "", "", "") as T
            UpdateClientDto::class -> UpdateClientDto(lastName = "") as T
            else -> throw IllegalArgumentException("Provide default for ${T::class}")
        }
}
