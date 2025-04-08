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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.*
import pack.userservicekotlin.arrow.UserServiceError
import pack.userservicekotlin.domain.dto.external.UserResponseDto
import pack.userservicekotlin.domain.dto.role.RoleRequestDto
import pack.userservicekotlin.service.UserService
import pack.userservicekotlin.utils.JwtTokenUtil

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var userService: UserService

    @MockitoBean
    lateinit var jwtTokenUtil: JwtTokenUtil

    @Test
    fun `getAllUsers should return page of users`() {
        val users = listOf(UserResponseDto(id = 1L, firstName = "John", lastName = "Doe", email = "j@example.com"))
        val page = PageImpl(users)

        `when`(userService.listUsers(PageRequest.of(0, 10))).thenReturn(page)

        mockMvc
            .get("/api/admin/users?page=0&size=10")
            .andExpect {
                status { isOk() }
                jsonPath("$.content[0].email") { value("j@example.com") }
            }
    }

    @Test
    fun `getUserRole should return role name`() {
        `when`(userService.getUserRole(1L)).thenReturn("AGENT".right())

        mockMvc
            .get("/api/admin/users/1/role")
            .andExpect {
                status { isOk() }
                content { string("AGENT") }
            }
    }

    @Test
    fun `getUserRole should return 404 if user not found`() {
        `when`(userService.getUserRole(1L)).thenReturn(UserServiceError.UserNotFound.left())

        mockMvc
            .get("/api/admin/users/1/role")
            .andExpect {
                status { isNotFound() }
                content { string("User not found") }
            }
    }

    @Test
    fun `addRoleToUser should return 200 when successful`() {
        val dto = RoleRequestDto(id = 1L)

        `when`(userService.addRoleToUser(Mockito.eq(1L), anyNonNull<RoleRequestDto>()))
            .thenReturn(Unit.right())

        mockMvc
            .post("/api/admin/users/1/role") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `addRoleToUser should return 409 if role already assigned`() {
        val dto = RoleRequestDto(id = 1L)

        `when`(userService.addRoleToUser(Mockito.eq(1L), anyNonNull<RoleRequestDto>()))
            .thenReturn(UserServiceError.RoleAlreadyAssigned.left())

        mockMvc
            .post("/api/admin/users/1/role") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(dto)
            }.andExpect {
                status { isConflict() }
            }
    }

    @Test
    fun `removeRoleFromUser should return 200 on success`() {
        `when`(userService.removeRoleFromUser(1L, 1L)).thenReturn(Unit.right())

        mockMvc
            .delete("/api/admin/users/1/role/1")
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `removeRoleFromUser should return 400 if role not assigned`() {
        `when`(userService.removeRoleFromUser(1L, 1L)).thenReturn(UserServiceError.RoleNotAssigned.left())

        mockMvc
            .delete("/api/admin/users/1/role/1")
            .andExpect {
                status { isBadRequest() }
            }
    }

    // I have no idea what's this chatGPT thing
    inline fun <reified T> anyNonNull(): T = Mockito.any(T::class.java) ?: createInstance()

    inline fun <reified T : Any> createInstance(): T =
        when (T::class) {
            RoleRequestDto::class -> RoleRequestDto() as T
            else -> throw IllegalArgumentException("Provide default for ${T::class}")
        }
}
