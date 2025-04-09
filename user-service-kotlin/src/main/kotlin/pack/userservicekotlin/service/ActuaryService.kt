package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.ActuaryServiceError
import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryLimitResponseDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.repository.ActuaryLimitRepository
import pack.userservicekotlin.repository.EmployeeRepository
import pack.userservicekotlin.specification.EmployeeSearchSpecification
import java.math.BigDecimal

@Service
class ActuaryService(
    private val actuaryLimitRepository: ActuaryLimitRepository,
    private val employeeRepository: EmployeeRepository,
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
                .and(EmployeeSearchSpecification.hasRole("AGENT"))

        return employeeRepository.findAll(spec, pageable).map { it.toDto() }
    }

    fun changeAgentLimit(
        employeeId: Long,
        newLimit: BigDecimal,
    ): Either<ActuaryServiceError, Unit> {
        val employee =
            employeeRepository.findById(employeeId).orElse(null)
                ?: return ActuaryServiceError.EmployeeNotFound(employeeId).left()

        if (employee.role?.name != "AGENT") {
            return ActuaryServiceError.NotAnAgent(employeeId).left()
        }

        val actuaryLimit =
            actuaryLimitRepository.findByEmployeeId(employeeId).orElse(null)
                ?: return ActuaryServiceError.ActuaryLimitNotFound(employeeId).left()

        actuaryLimit.limitAmount = newLimit
        actuaryLimitRepository.save(actuaryLimit)
        return Unit.right()
    }

    fun resetDailyLimit(employeeId: Long): Either<ActuaryServiceError, Unit> {
        val employee =
            employeeRepository.findById(employeeId).orElse(null)
                ?: return ActuaryServiceError.EmployeeNotFound(employeeId).left()

        if (employee.role?.name != "AGENT") {
            return ActuaryServiceError.NotAnAgent(employeeId).left()
        }

        val actuaryLimit =
            actuaryLimitRepository.findByEmployeeId(employeeId).orElse(null)
                ?: return ActuaryServiceError.ActuaryLimitNotFound(employeeId).left()

        actuaryLimit.usedLimit = BigDecimal.ZERO
        actuaryLimitRepository.save(actuaryLimit)
        return Unit.right()
    }

    fun setApproval(
        employeeId: Long,
        value: Boolean,
    ): Either<ActuaryServiceError, Unit> {
        val employee =
            employeeRepository.findById(employeeId).orElse(null)
                ?: return ActuaryServiceError.EmployeeNotFound(employeeId).left()

        if (employee.role?.name != "AGENT") {
            return ActuaryServiceError.NotAnAgent(employeeId).left()
        }

        val actuaryLimit =
            actuaryLimitRepository.findByEmployeeId(employeeId).orElse(null)
                ?: return ActuaryServiceError.ActuaryLimitNotFound(employeeId).left()

        actuaryLimit.needsApproval = value
        actuaryLimitRepository.save(actuaryLimit)
        return Unit.right()
    }

    fun getAgentLimit(id: Long): Either<ActuaryServiceError, ActuaryLimitResponseDto> {
        val actuaryLimit =
            actuaryLimitRepository.findByEmployeeId(id).orElse(null)
                ?: return ActuaryServiceError.ActuaryLimitNotFound(id).left()

        return ActuaryLimitResponseDto(
            limitAmount = actuaryLimit.limitAmount,
            usedLimit = actuaryLimit.usedLimit,
            needsApproval = actuaryLimit.needsApproval,
        ).right()
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun resetDailyLimits() {
        val limits = actuaryLimitRepository.findAll()
        limits.forEach { it?.usedLimit = BigDecimal.ZERO }
        println("Daily used limits have been reset.")
        actuaryLimitRepository.saveAll(limits)
    }
}
