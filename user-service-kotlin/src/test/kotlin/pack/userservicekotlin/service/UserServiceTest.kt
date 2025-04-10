package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import pack.userservicekotlin.arrow.UserServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.role.RoleRequestDto
import pack.userservicekotlin.domain.entities.BaseUser
import pack.userservicekotlin.repository.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock lateinit var userRepository: UserRepository

    @Mock lateinit var roleRepository: RoleRepository

    @Mock lateinit var permissionRepository: PermissionRepository

    @Mock lateinit var actuaryLimitRepository: ActuaryLimitRepository

    @Mock lateinit var pageable: Pageable

    @InjectMocks
    lateinit var userService: UserService

    @Test
    fun `getUserRole returns role name when assigned`() {
        val user = TestDataFactory.employee(id = 1L)
        user.role = TestDataFactory.role("AGENT")

        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))

        val result = userService.getUserRole(1L)

        assertTrue(result.isRight())
        assertEquals("AGENT", result.getOrNull())
    }

    @Test
    fun `getUserRole returns RoleNotAssigned when user has no role`() {
        val user = TestDataFactory.employee(id = 2L)
        user.role = null

        `when`(userRepository.findById(2L)).thenReturn(Optional.of(user))

        val result = userService.getUserRole(2L)

        assertTrue(result.isLeft())
        assertEquals(UserServiceError.RoleNotAssigned, result.swap().getOrNull())
    }

    @Test
    fun `getUserRole returns UserNotFound when user does not exist`() {
        `when`(userRepository.findById(3L)).thenReturn(Optional.empty())

        val result = userService.getUserRole(3L)

        assertTrue(result.isLeft())
        assertEquals(UserServiceError.UserNotFound, result.swap().getOrNull())
    }

    @Test
    fun `addRoleToUser assigns new role successfully`() {
        val user = TestDataFactory.employee(id = 4L)
        user.role = TestDataFactory.role("CLIENT")

        val newRole = TestDataFactory.role("AGENT").apply { id = 2L }
        val roleDto = RoleRequestDto(id = 2L)

        `when`(userRepository.findById(4L)).thenReturn(Optional.of(user))
        `when`(roleRepository.findById(2L)).thenReturn(Optional.of(newRole))
        `when`(userRepository.save(user)).thenReturn(user)

        val result = userService.addRoleToUser(4L, roleDto)

        assertTrue(result.isRight())
        assertEquals("AGENT", user.role?.name)
    }

    @Test
    fun `addRoleToUser returns RoleAlreadyAssigned if same role`() {
        val role = TestDataFactory.role("AGENT").apply { id = 5L }
        val user = TestDataFactory.employee(id = 5L)
        user.role = role

        val dto = RoleRequestDto(id = 5L)

        `when`(userRepository.findById(5L)).thenReturn(Optional.of(user))
        `when`(roleRepository.findById(5L)).thenReturn(Optional.of(role))

        val result = userService.addRoleToUser(5L, dto)

        assertTrue(result.isLeft())
        assertEquals(UserServiceError.RoleAlreadyAssigned, result.swap().getOrNull())
    }

    @Test
    fun `removeRoleFromUser removes role successfully`() {
        val role = TestDataFactory.role("AGENT").apply { id = 6L }
        val user = TestDataFactory.employee(id = 6L)
        user.role = role

        `when`(userRepository.findById(6L)).thenReturn(Optional.of(user))
        `when`(roleRepository.findById(6L)).thenReturn(Optional.of(role))
        `when`(userRepository.save(user)).thenReturn(user)

        val result = userService.removeRoleFromUser(6L, 6L)

        assertTrue(result.isRight())
        assertNull(user.role)
    }

    @Test
    fun `removeRoleFromUser returns RoleNotAssigned if names mismatch`() {
        val user = TestDataFactory.employee(id = 7L)
        user.role = TestDataFactory.role("CLIENT")

        val role = TestDataFactory.role("AGENT").apply { id = 7L }

        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        `when`(roleRepository.findById(7L)).thenReturn(Optional.of(role))

        val result = userService.removeRoleFromUser(7L, 7L)

        assertTrue(result.isLeft())
        assertEquals(UserServiceError.RoleNotAssigned, result.swap().getOrNull())
    }

    @Test
    fun `listUsers returns user page mapped to DTO`() {
        val user1 = TestDataFactory.employee(id = 10L)
        val user2 = TestDataFactory.employee(id = 11L)
        val page: Page<BaseUser> = PageImpl(listOf(user1, user2))

        `when`(userRepository.findAll(pageable)).thenReturn(page)

        val result = userService.listUsers(pageable)

        assertEquals(2, result.content.size)
        assertEquals(user1.id, result.content[0].id)
    }
}
