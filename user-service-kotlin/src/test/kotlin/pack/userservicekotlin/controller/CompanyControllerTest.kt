package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.CompanyServiceError
import pack.userservicekotlin.domain.TestRequestData.validCreateCompanyDto
import pack.userservicekotlin.domain.TestResponseData.validCompanyResponseDto
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto
import pack.userservicekotlin.service.CompanyService
import pack.userservicekotlin.utils.JwtTokenUtil

@ExtendWith(SpringExtension::class)
@WebMvcTest(CompanyController::class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var companyService: CompanyService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 201 when successful`() {
        val request = validCreateCompanyDto()
        val response = validCompanyResponseDto()

        `when`(companyService.createCompany(anyNonNull()))
            .thenReturn(Either.Right(response))

        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.name") { value(response.name) }
            }
    }

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 404 when owner not found`() {
        val dto = validCreateCompanyDto()

        `when`(companyService.createCompany(anyNonNull()))
            .thenReturn(Either.Left(CompanyServiceError.OwnerNotFound(1L)))

        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isNotFound() }
                content { string("Owner not found: 1") }
            }
    }

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 404 when activity code not found`() {
        val dto = validCreateCompanyDto()

        `when`(companyService.createCompany(anyNonNull()))
            .thenReturn(Either.Left(CompanyServiceError.ActivityCodeNotFound("1L")))

        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isNotFound() }
                content { string("Activity code not found: 1L") }
            }
    }

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 409 when registration number exists`() {
        val dto = validCreateCompanyDto()

        `when`(companyService.createCompany(anyNonNull()))
            .thenReturn(Either.Left(CompanyServiceError.RegistrationNumberExists("123456789")))

        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isConflict() }
                content { string("Company registration number exists: 123456789") }
            }
    }

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 409 when tax ID exists`() {
        val dto = validCreateCompanyDto()

        `when`(companyService.createCompany(anyNonNull()))
            .thenReturn(Either.Left(CompanyServiceError.TaxIdExists("987654321")))

        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isConflict() }
                content { string("Company tax ID exists: 987654321") }
            }
    }

    @WithMockUser(roles = ["EMPLOYEE"])
    @Test
    fun `createCompany should return 400 if invalid request body`() {
        mockMvc
            .post("/api/company") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @WithMockUser
    @Test
    fun `getCompanyById should return 404 when not found`() {
        `when`(companyService.getCompanyById(1L))
            .thenReturn(Either.Left(CompanyServiceError.CompanyNotFound(1L)))

        mockMvc
            .get("/api/company/1")
            .andExpect {
                status { isNotFound() }
                content { string("Company not found with ID: 1") }
            }
    }

    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `getCompaniesForClientId should return 200 with companies`() {
        val response = listOf(validCompanyResponseDto())

        `when`(companyService.getCompaniesForClientId(1L)).thenReturn(response)

        mockMvc
            .get("/api/company/owned-by/1")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].name") { value(response[0].name) }
            }
    }

    @Test
    @WithMockUser(roles = ["EMPLOYEE"])
    fun `getCompaniesForClientId should return empty list when no companies exist`() {
        `when`(companyService.getCompaniesForClientId(1L)).thenReturn(emptyList())

        mockMvc
            .get("/api/company/owned-by/1")
            .andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$.length()") { value(0) }
            }
    }

    inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java) ?: createInstance()

    inline fun <reified T : Any> createInstance(): T =
        when (T::class) {
            CreateCompanyDto::class ->
                CreateCompanyDto(
                    name = "",
                    registrationNumber = "",
                    taxId = "",
                    activityCode = "",
                    address = "",
                    majorityOwnerId = 1L,
                ) as T
            else -> throw IllegalArgumentException("Provide default for ${T::class}")
        }
}
