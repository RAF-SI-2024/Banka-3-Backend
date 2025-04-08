package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.AuthorizedPersonnelServiceError
import pack.userservicekotlin.domain.dto.authorized_presonnel.AuthorizedPersonnelResponseDto
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.service.AuthorizedPersonnelService
import pack.userservicekotlin.utils.JwtTokenUtil

@WebMvcTest(AuthorizedPersonnelController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthorizedPersonnelControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @MockitoBean
    private lateinit var authorizedPersonnelService: AuthorizedPersonnelService

    @Test
    fun `createAuthorizedPersonnel should return 201 when personnel is created`() {
        val request =
            CreateAuthorizedPersonnelDto(
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                companyId = 1L,
            )
        val response =
            AuthorizedPersonnelResponseDto(
                id = 1L,
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                companyId = 1L,
            )

        `when`(authorizedPersonnelService.createAuthorizedPersonnel(request))
            .thenReturn(Either.Right(response))

        mockMvc
            .post("/api/authorized-personnel") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(1L) }
                jsonPath("$.firstName") { value("John") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("john.doe@example.com") }
            }
    }

    @Test
    fun `createAuthorizedPersonnel should return 400 when company is not found`() {
        val request =
            CreateAuthorizedPersonnelDto(
                firstName = "Jane",
                lastName = "Doe",
                email = "jane.doe@example.com",
                companyId = 2L,
            )

        `when`(authorizedPersonnelService.createAuthorizedPersonnel(request))
            .thenReturn(Either.Left(AuthorizedPersonnelServiceError.CompanyNotFound(2L)))

        mockMvc
            .post("/api/authorized-personnel") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
                content { string("Company not found with id: 2") }
            }
    }

    @Test
    fun `getAuthorizedPersonnelByCompany should return personnel list when company exists`() {
        val companyId = 1L
        val response =
            listOf(
                AuthorizedPersonnelResponseDto(
                    id = 1L,
                    firstName = "John",
                    lastName = "Doe",
                    email = "john.doe@example.com",
                    companyId = companyId,
                ),
            )

        `when`(authorizedPersonnelService.getAuthorizedPersonnelByCompany(companyId))
            .thenReturn(Either.Right(response))

        mockMvc
            .get("/api/authorized-personnel/company/{companyId}", companyId)
            .andExpect {
                status { isOk() }
                jsonPath("$.size()") { value(1) }
                jsonPath("$[0].firstName") { value("John") }
            }
    }

    @Test
    fun `getAuthorizedPersonnelByCompany should return 404 when company is not found`() {
        val companyId = 2L

        `when`(authorizedPersonnelService.getAuthorizedPersonnelByCompany(companyId))
            .thenReturn(Either.Left(AuthorizedPersonnelServiceError.CompanyNotFound(companyId)))

        mockMvc
            .get("/api/authorized-personnel/company/{companyId}", companyId)
            .andExpect {
                status { isNotFound() }
                content { string("Company not found with id: 2") }
            }
    }

    @Test
    fun `getAuthorizedPersonnelById should return personnel when found`() {
        val personnelId = 1L
        val response =
            AuthorizedPersonnelResponseDto(
                id = personnelId,
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                companyId = 1L,
            )

        `when`(authorizedPersonnelService.getAuthorizedPersonnelById(personnelId))
            .thenReturn(Either.Right(response))

        mockMvc
            .get("/api/authorized-personnel/{id}", personnelId)
            .andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("John") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("john.doe@example.com") }
            }
    }

    @Test
    fun `getAuthorizedPersonnelById should return 404 when personnel is not found`() {
        val personnelId = 2L

        `when`(authorizedPersonnelService.getAuthorizedPersonnelById(personnelId))
            .thenReturn(Either.Left(AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(personnelId)))

        mockMvc
            .get("/api/authorized-personnel/{id}", personnelId)
            .andExpect {
                status { isNotFound() }
                content { string("Authorized personnel not found with id: 2") }
            }
    }

    @Test
    fun `updateAuthorizedPersonnel should return updated personnel when successful`() {
        val personnelId = 1L
        val request =
            CreateAuthorizedPersonnelDto(
                firstName = "Johnny",
                lastName = "Doe",
                email = "johnny.doe@example.com",
                companyId = 1L,
            )
        val response =
            AuthorizedPersonnelResponseDto(
                id = personnelId,
                firstName = "Johnny",
                lastName = "Doe",
                email = "johnny.doe@example.com",
                companyId = 1L,
            )

        `when`(authorizedPersonnelService.updateAuthorizedPersonnel(personnelId, request))
            .thenReturn(Either.Right(response))

        mockMvc
            .put("/api/authorized-personnel/{id}", personnelId) {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.firstName") { value("Johnny") }
                jsonPath("$.lastName") { value("Doe") }
                jsonPath("$.email") { value("johnny.doe@example.com") }
            }
    }
}
