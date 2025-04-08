package pack.userservicekotlin.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import pack.userservicekotlin.arrow.UserServiceError
import pack.userservicekotlin.domain.dto.external.UserResponseDto
import pack.userservicekotlin.domain.dto.role.RoleRequestDto
import pack.userservicekotlin.service.UserService
import pack.userservicekotlin.swagger.UserApiDoc

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController(
    private val userService: UserService,
) : UserApiDoc {
    @GetMapping
    override fun getAllUsers(
        page: Int,
        size: Int,
    ): ResponseEntity<Page<UserResponseDto>> {
        val pageable: Pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(userService.listUsers(pageable))
    }

    @GetMapping("/{userId}/role")
    override fun getUserRole(userId: Long): ResponseEntity<String> =
        userService.getUserRole(userId).fold(
            ifLeft = { error ->
                when (error) {
                    UserServiceError.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found")
                    UserServiceError.RoleNotAssigned -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role not assigned to user")
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { role -> ResponseEntity.ok(role) },
        )

    @PostMapping("/{userId}/role")
    override fun addRoleToUser(
        userId: Long,
        roleRequestDto: RoleRequestDto,
    ): ResponseEntity<Void> =
        userService.addRoleToUser(userId, roleRequestDto).fold(
            ifLeft = { error ->
                when (error) {
                    UserServiceError.UserNotFound,
                    UserServiceError.RoleNotFound,
                    -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    UserServiceError.RoleAlreadyAssigned -> ResponseEntity.status(HttpStatus.CONFLICT).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )

    @DeleteMapping("/{userId}/role/{roleId}")
    override fun removeRoleFromUser(
        userId: Long,
        roleId: Long,
    ): ResponseEntity<Void> =
        userService.removeRoleFromUser(userId, roleId).fold(
            ifLeft = { error ->
                when (error) {
                    UserServiceError.UserNotFound,
                    UserServiceError.RoleNotFound,
                    -> ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                    UserServiceError.RoleNotAssigned -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
                    else -> ResponseEntity.internalServerError().build()
                }
            },
            ifRight = { ResponseEntity.ok().build() },
        )
}
