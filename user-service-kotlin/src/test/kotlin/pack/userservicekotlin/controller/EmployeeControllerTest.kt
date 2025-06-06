package pack.userservicekotlin.controller

import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.EmployeeServiceError
import pack.userservicekotlin.domain.TestRequestData.validCreateEmployeeDto
import pack.userservicekotlin.domain.TestRequestData.validUpdateEmployeeDto
import pack.userservicekotlin.domain.TestResponseData
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.service.EmployeeService
import pack.userservicekotlin.utils.JwtTokenUtil

@WebMvcTest(EmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var employeeService: EmployeeService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @Test
    fun `getEmployeeById should return 200 when found`() {
        val employee = TestResponseData.validEmployeeResponseDto()

        `when`(employeeService.findById(1L)).thenReturn(employee.right())

        mockMvc
            .get("/api/admin/employees/1")
            .andExpect {
                status { isOk() }
                jsonPath("$.email") { value("john.doe@example.com") }
            }
    }

    @Test
    fun `getEmployeeById should return 404 when not found`() {
        `when`(employeeService.findById(1L)).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .get("/api/admin/employees/1")
            .andExpect {
                status { isNotFound() }
                content { string("Employee not found") }
            }
    }

    @Test
    fun `createEmployee should return 201 when successful`() {
        val dto = validCreateEmployeeDto()
        val response = TestResponseData.validEmployeeResponseDto()

        `when`(employeeService.createEmployee(anyNonNull())).thenReturn(response.right())

        mockMvc
            .post("/api/admin/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isCreated() }
                jsonPath("$.email") { value("john.doe@example.com") }
            }
    }

    @Test
    fun `updateEmployee should return 200 when successful`() {
        val dto = validUpdateEmployeeDto()
        val response = TestResponseData.validEmployeeResponseDto()

        `when`(employeeService.updateEmployee(eq(1L), anyNonNull())).thenReturn(response.right())

        mockMvc
            .put("/api/admin/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isOk() }
                jsonPath("$.position") { value("Developer") }
            }
    }

    @Test
    fun `deleteEmployee should return 200`() {
        `when`(employeeService.deleteEmployee(1L)).thenReturn(Unit.right())

        mockMvc
            .delete("/api/admin/employees/1")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `deactivateEmployee should return 200`() {
        `when`(employeeService.deactivateEmployee(1L)).thenReturn(Unit.right())

        mockMvc
            .patch("/api/admin/employees/1/deactivate")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `activateEmployee should return 200 when success`() {
        `when`(employeeService.activateEmployee(1L)).thenReturn(Unit.right())

        mockMvc
            .patch("/api/admin/employees/1/activate")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `activateEmployee should return 404 when not found`() {
        `when`(employeeService.activateEmployee(1L)).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .patch("/api/admin/employees/1/activate")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `getCurrentEmployee should return 200`() {
        val email = "john.doe@example.com"
        val response = TestResponseData.validEmployeeResponseDto()

        val auth = mock(org.springframework.security.core.Authentication::class.java)
        `when`(auth.name).thenReturn(email)
        val context = mock(org.springframework.security.core.context.SecurityContext::class.java)
        `when`(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        `when`(employeeService.findByEmail(email)).thenReturn(response.right())

        mockMvc
            .get("/api/admin/employees/me")
            .andExpect {
                status { isOk() }
                jsonPath("$.email") { value(email) }
            }
    }

    @Test
    fun `getCurrentEmployee should return 404 if employee not found`() {
        val email = "nonexistent@example.com"
        val auth = mock(org.springframework.security.core.Authentication::class.java)
        `when`(auth.name).thenReturn(email)
        val context = mock(org.springframework.security.core.context.SecurityContext::class.java)
        `when`(context.authentication).thenReturn(auth)
        SecurityContextHolder.setContext(context)

        `when`(employeeService.findByEmail(email)).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .get("/api/admin/employees/me")
            .andExpect {
                status { isNotFound() }
                content { string("Employee not found") }
            }
    }

    @Test
    fun `createEmployee should return 404 if role not found`() {
        val dto = validCreateEmployeeDto()
        `when`(employeeService.createEmployee(anyNonNull())).thenReturn(EmployeeServiceError.RoleNotFound.left())

        mockMvc
            .post("/api/admin/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isNotFound() }
                content { string("Role not found") }
            }
    }

    @Test
    fun `createEmployee should return 409 if email already exists`() {
        val dto = validCreateEmployeeDto()
        `when`(employeeService.createEmployee(anyNonNull())).thenReturn(EmployeeServiceError.EmailAlreadyExists.left())

        mockMvc
            .post("/api/admin/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isConflict() }
            }
    }

    @Test
    fun `createEmployee should return 400 if invalid request body`() {
        mockMvc
            .post("/api/admin/employees") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `updateEmployee should return 404 if employee not found`() {
        val dto = validUpdateEmployeeDto()
        `when`(employeeService.updateEmployee(eq(1L), anyNonNull())).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .put("/api/admin/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isNotFound() }
                content { string("Employee not found") }
            }
    }

    @Test
    fun `updateEmployee should return 404 if role not found`() {
        val dto = validUpdateEmployeeDto()
        `when`(employeeService.updateEmployee(eq(1L), anyNonNull())).thenReturn(EmployeeServiceError.RoleNotFound.left())

        mockMvc
            .put("/api/admin/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isNotFound() }
                content { string("Role not found") }
            }
    }

    @Test
    fun `updateEmployee should return 400 if invalid request body`() {
        mockMvc
            .put("/api/admin/employees/1") {
                contentType = MediaType.APPLICATION_JSON
                content = "invalid json"
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `deleteEmployee should return 404 if not found`() {
        `when`(employeeService.deleteEmployee(1L)).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .delete("/api/admin/employees/1")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `deactivateEmployee should return 404 if not found`() {
        `when`(employeeService.deactivateEmployee(1L)).thenReturn(EmployeeServiceError.NotFound.left())

        mockMvc
            .patch("/api/admin/employees/1/deactivate")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `getCurrentEmployee should return 401 if not authenticated`() {
        SecurityContextHolder.clearContext()

        mockMvc
            .get("/api/admin/employees/me")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java) ?: createInstance()

    inline fun <reified T : Any> createInstance(): T =
        when (T::class) {
            CreateEmployeeDto::class -> CreateEmployeeDto() as T
            UpdateEmployeeDto::class ->
                UpdateEmployeeDto(
                    lastName = "",
                    gender = "",
                    phone = "",
                    address = "",
                    position = "",
                    department = "",
                    role = "",
                ) as T
            else -> throw IllegalArgumentException("Provide default for ${T::class}")
        }
}
