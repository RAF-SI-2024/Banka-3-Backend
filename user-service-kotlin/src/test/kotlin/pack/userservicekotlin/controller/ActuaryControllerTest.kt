package pack.userservicekotlin.controller

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.ActuaryServiceError
import pack.userservicekotlin.domain.dto.activity_code.SetApprovalDto
import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryLimitResponseDto
import pack.userservicekotlin.domain.dto.actuary_limit.UpdateActuaryLimitDto
import pack.userservicekotlin.domain.dto.employee.AgentDto
import pack.userservicekotlin.service.ActuaryService
import pack.userservicekotlin.utils.JwtTokenUtil
import java.math.BigDecimal
import java.util.*

@WebMvcTest(ActuaryController::class)
@AutoConfigureMockMvc(addFilters = false)
class ActuaryControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var actuaryService: ActuaryService

    @MockitoBean lateinit var jwtTokenUtil: JwtTokenUtil

    @Test
    fun `changeAgentLimit should return 200 when successful`() {
        val request = UpdateActuaryLimitDto(BigDecimal(500.0))
        `when`(actuaryService.changeAgentLimit(1L, request.newLimit!!)).thenReturn(Either.Right(Unit))

        mockMvc
            .put("/api/admin/actuaries/change-limit/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `resetDailyLimit should return 200 when successful`() {
        `when`(actuaryService.resetDailyLimit(1L)).thenReturn(Either.Right(Unit))

        mockMvc
            .put("/api/admin/actuaries/reset-limit/1")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `setApprovalValue should return 200 when successful`() {
        val request = SetApprovalDto(needApproval = true)
        `when`(actuaryService.setApproval(1L, true)).thenReturn(Either.Right(Unit))

        mockMvc
            .put("/api/admin/actuaries/set-approval/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `getAllAgents should return paginated agents`() {
        val agents =
            listOf(
                AgentDto(
                    1L,
                    "username",
                    "position",
                    "department",
                    true,
                    "A",
                    "B",
                    "a@b.com",
                    "123",
                    Date(),
                    "M",
                    "123-456",
                    "Some Street",
                    "AGENT",
                    BigDecimal(0),
                    BigDecimal(0),
                    false,
                ),
            )
        val page = PageImpl(agents)

        `when`(actuaryService.findAgents(null, null, null, null, PageRequest.of(0, 10))).thenReturn(Either.Right(page))

        mockMvc
            .get("/api/admin/actuaries/agents?page=0&size=10")
            .andExpect {
                status { isOk() }
                jsonPath("$.content[0].email") { value("a@b.com") }
            }
    }

    @Test
    fun `getAgentLimit should return 200 when found`() {
        val response = ActuaryLimitResponseDto(BigDecimal(100.0), BigDecimal(0), false)
        `when`(actuaryService.getAgentLimit(1L)).thenReturn(Either.Right(response))

        mockMvc
            .get("/api/admin/actuaries/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.limitAmount") { value(100.0) }
            }
    }

    @Test
    fun `getAgentLimit should return 404 when not found`() {
        `when`(actuaryService.getAgentLimit(1L))
            .thenReturn(Either.Left(ActuaryServiceError.ActuaryLimitNotFound(1L)))

        mockMvc
            .get("/api/admin/actuaries/1")
            .andExpect {
                status { isNotFound() }
                content { string("Limit not found for agent 1") }
            }
    }

    inline fun <reified T> anyNonNull(): T =
        try {
            Mockito.any(T::class.java) ?: createInstance()
        } catch (e: Exception) {
            createInstance()
        }

    inline fun <reified T : Any> createInstance(): T =
        when (T::class) {
            UpdateActuaryLimitDto::class -> UpdateActuaryLimitDto(BigDecimal(0.0)) as T
            SetApprovalDto::class -> SetApprovalDto(false) as T
            else -> throw IllegalArgumentException("Provide default for ${T::class}")
        }
}
