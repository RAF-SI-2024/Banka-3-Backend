package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.UserServiceError
import pack.userservicekotlin.domain.dto.external.UserResponseDto
import pack.userservicekotlin.domain.dto.role.RoleRequestDto
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.repository.ActuaryLimitRepository
import pack.userservicekotlin.repository.PermissionRepository
import pack.userservicekotlin.repository.RoleRepository
import pack.userservicekotlin.repository.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val actuaryLimitRepository: ActuaryLimitRepository,
) {
    fun getUserRole(userId: Long): Either<UserServiceError, String> {
        return userRepository
            .findById(userId)
            .map { user ->
                user.role?.name?.right()
                    ?: UserServiceError.RoleNotAssigned.left()
            }.orElse(null) ?: return UserServiceError.UserNotFound.left()
    }

    fun addRoleToUser(
        userId: Long,
        roleRequestDto: RoleRequestDto,
    ): Either<UserServiceError, Unit> {
        val user =
            userRepository
                .findById(userId)
                .orElse(null) ?: return UserServiceError.UserNotFound.left()

        val role =
            roleRepository
                .findById(roleRequestDto.id!!)
                .orElse(null) ?: return UserServiceError.RoleNotFound.left()

        if (user.role?.name.equals(role.name, ignoreCase = true)) {
            return UserServiceError.RoleAlreadyAssigned.left()
        }

        user.role = role
        userRepository.save(user)
        return Unit.right()
    }

    fun removeRoleFromUser(
        userId: Long,
        roleId: Long,
    ): Either<UserServiceError, Unit> {
        val user =
            userRepository
                .findById(userId)
                .orElse(null) ?: return UserServiceError.UserNotFound.left()

        val role =
            roleRepository
                .findById(roleId)
                .orElse(null) ?: return UserServiceError.RoleNotFound.left()

        if (!user.role?.name.equals(role.name, ignoreCase = true)) {
            return UserServiceError.RoleNotAssigned.left()
        }

        user.role = null
        userRepository.save(user)
        return Unit.right()
    }

    fun listUsers(pageable: Pageable): Page<UserResponseDto> = userRepository.findAll(pageable).map { it.toDto() }
}
