package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.EmployeeServiceError
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.entities.ActuaryLimit
import pack.userservicekotlin.domain.entities.AuthToken
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.*
import pack.userservicekotlin.specification.EmployeeSearchSpecification
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val roleRepository: RoleRepository,
    private val actuaryLimitRepository: ActuaryLimitRepository,
) {
    fun findAll(
        firstName: String?,
        lastName: String?,
        email: String?,
        position: String?,
        pageable: Pageable,
    ): Page<EmployeeResponseDto> {
        val spec =
            Specification
                .where(EmployeeSearchSpecification.startsWithFirstName(firstName))
                .and(EmployeeSearchSpecification.startsWithLastName(lastName))
                .and(EmployeeSearchSpecification.startsWithEmail(email))
                .and(EmployeeSearchSpecification.startsWithPosition(position))

        return employeeRepository.findAll(spec, pageable).map { it.toDto() }
    }

    fun findById(id: Long): Either<EmployeeServiceError, EmployeeResponseDto> =
        employeeRepository
            .findById(id)
            .map { it.toDto()!! }
            .orElse(null)
            ?.right()
            ?: EmployeeServiceError.NotFound.left()

    fun deleteEmployee(id: Long): Either<EmployeeServiceError, Unit> =
        employeeRepository
            .findById(id)
            .orElse(null)
            ?.let {
                employeeRepository.delete(it)
                Unit.right()
            } ?: EmployeeServiceError.NotFound.left()

    fun deactivateEmployee(id: Long): Either<EmployeeServiceError, Unit> =
        employeeRepository
            .findById(id)
            .orElse(null)
            ?.let {
                it.active = false
                employeeRepository.save(it)
                Unit.right()
            } ?: EmployeeServiceError.NotFound.left()

    fun activateEmployee(id: Long): Either<EmployeeServiceError, Unit> =
        employeeRepository
            .findById(id)
            .orElse(null)
            ?.let {
                it.active = true
                employeeRepository.save(it)
                Unit.right()
            } ?: EmployeeServiceError.NotFound.left()

    fun createEmployee(dto: CreateEmployeeDto): Either<EmployeeServiceError, EmployeeResponseDto> {
        val role =
            roleRepository.findByName(dto.role!!).orElse(null)
                ?: return EmployeeServiceError.RoleNotFound.left()

        val employee = dto.toEntity() ?: return EmployeeServiceError.Unknown(NullPointerException("Invalid entity")).left()
        employee.role = role
        employeeRepository.save(employee)

        val token = UUID.randomUUID().toString()
        rabbitTemplate.convertAndSend("set-password", EmailRequestDto(token, employee.email!!))

        val createdAt = Instant.now().toEpochMilli()
        val expiresAt = createdAt + 86400000
        authTokenRepository.save(
            AuthToken(
                createdAt = createdAt,
                expiresAt = expiresAt,
                token = token,
                type = "set-password",
                userId = employee.id,
            ),
        )

        return employee.toDto()!!.right()
    }

    fun updateEmployee(
        id: Long,
        dto: UpdateEmployeeDto,
    ): Either<EmployeeServiceError, EmployeeResponseDto> {
        val employee =
            employeeRepository.findById(id).orElse(null)
                ?: return EmployeeServiceError.NotFound.left()

        val role =
            roleRepository.findByName(dto.role!!).orElse(null)
                ?: return EmployeeServiceError.RoleNotFound.left()

        employee.apply {
            lastName = dto.lastName
            gender = dto.gender
            phone = dto.phone
            address = dto.address
            position = dto.position
            department = dto.department
        }

        if (role.name == "AGENT" && employee.role?.name != "AGENT") {
            actuaryLimitRepository.save(
                ActuaryLimit(
                    limitAmount = BigDecimal(100000),
                    usedLimit = BigDecimal.ZERO,
                    needsApproval = true,
                    employee = employee,
                ),
            )
        }

        if (role.name != "AGENT" && employee.role?.name == "AGENT") {
            val limit =
                actuaryLimitRepository
                    .findByEmployeeId(id)
                    .orElse(null) ?: return EmployeeServiceError.LimitNotFound.left()
            actuaryLimitRepository.delete(limit)
        }

        employee.role = role
        return employeeRepository.save(employee).toDto()!!.right()
    }

    fun findByEmail(email: String): Either<EmployeeServiceError, EmployeeResponseDto> =
        employeeRepository
            .findByEmail(email)
            .map { it.toDto()!! }
            .orElse(null)
            ?.right()
            ?: EmployeeServiceError.NotFound.left()
}
