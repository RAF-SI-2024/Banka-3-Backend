package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.ActuaryServiceError
import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryLimitResponseDto
import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryResponseDto
import pack.userservicekotlin.domain.dto.employee.AgentDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.dto.external.OrderDto
import pack.userservicekotlin.domain.entities.Employee
import pack.userservicekotlin.domain.mapper.toActuaryDto
import pack.userservicekotlin.domain.mapper.toAgentDto
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.external.StockClient
import pack.userservicekotlin.repository.ActuaryLimitRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.EmployeeRepository
import pack.userservicekotlin.specification.ClientSearchSpecification
import pack.userservicekotlin.specification.ClientSearchSpecification.andIf
import pack.userservicekotlin.specification.EmployeeSearchSpecification
import java.math.BigDecimal

@Service
class ActuaryService(
    private val actuaryLimitRepository: ActuaryLimitRepository,
    private val employeeRepository: EmployeeRepository,
    private val clientRepository: ClientRepository,
    private val stockClient: StockClient,
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

    fun findActuaries(pageable: Pageable?): Either<ActuaryServiceError, Page<ActuaryResponseDto>> {
        if (pageable == null) return ActuaryServiceError.NotAnAgent(1).left()

        val spec: Specification<Employee?> =
            Specification.where(
                EmployeeSearchSpecification
                    .hasRole("AGENT")
                    .or(EmployeeSearchSpecification.hasRole("SUPERVISOR"))
                    .or(EmployeeSearchSpecification.hasRole("ADMIN")),
            )

        val actuaryDtoPage: Page<ActuaryResponseDto> =
            employeeRepository
                .findAll(spec, pageable)
                .map { employee ->
                    employee.toActuaryDto() ?: ActuaryResponseDto(
                        id = employee.id ?: -1,
                        firstName = employee.firstName ?: "",
                        lastName = employee.lastName ?: "",
                        role = employee.role?.name ?: "UNKNOWN",
                        profit = BigDecimal.ZERO,
                    )
                }

        val orderDtos: List<OrderDto> =
            try {
                stockClient.getAll()
            } catch (e: Exception) {
                return ActuaryServiceError.ExternalServiceError("Stock client failure: ${e.message}").left()
            }

        val enrichedContent =
            actuaryDtoPage.content.map { actuary ->
                val totalProfit =
                    orderDtos
                        .filter { it.userId == actuary.id }
                        .fold(actuary.profit) { acc, order -> acc.add(order.profit) }

                actuary.copy(profit = totalProfit)
            }

        val enrichedPage =
            PageImpl(
                enrichedContent,
                actuaryDtoPage.pageable,
                actuaryDtoPage.totalElements,
            )

        return enrichedPage.right()
    }

    fun findAgents(
        firstName: String?,
        lastName: String?,
        email: String?,
        position: String?,
        pageable: Pageable,
    ): Either<ActuaryServiceError, Page<AgentDto>> {
        val spec =
            Specification
                .where(EmployeeSearchSpecification.startsWithFirstName(firstName))
                .and(EmployeeSearchSpecification.startsWithLastName(lastName))
                .and(EmployeeSearchSpecification.startsWithEmail(email))
                .and(EmployeeSearchSpecification.startsWithPosition(position))
                .and(EmployeeSearchSpecification.hasRole("AGENT"))


        val employeeDtoPage =
            employeeRepository
                .findAll(spec, pageable)
                .map { it.toDto() }


        val agentList = mutableListOf<AgentDto>()

        for (employeeDto in employeeDtoPage.content) {
            val actuaryLimit =
                actuaryLimitRepository
                    .findByEmployeeId(employeeDto?.id!!)
                    .orElse(null)
                    ?: return ActuaryServiceError.ActuaryLimitNotFound(employeeDto.id).left()

            val agentDto =
                employeeDto.toAgentDto(
                    actuaryLimit.limitAmount!!,
                    actuaryLimit.usedLimit!!,
                    actuaryLimit.needsApproval,
                )

            agentList.add(agentDto!!)
        }

        return PageImpl(
            agentList,
            employeeDtoPage.pageable,
            employeeDtoPage.totalElements,
        ).right()
    }

    fun getAllAgentsAndClients(
        name: String,
        surname: String,
        role: String,
    ): Either<ActuaryServiceError, List<ActuaryResponseDto>> {
        val specEmployee =
            if (role.isNotEmpty()) {
                Specification.where(EmployeeSearchSpecification.hasRole(role))
            } else {
                Specification
                    .where(EmployeeSearchSpecification.hasRole("AGENT"))
                    .or(EmployeeSearchSpecification.hasRole("SUPERVISOR"))
                    .or(EmployeeSearchSpecification.hasRole("ADMIN"))
            }

        val finalEmployeeSpec =
            specEmployee
                .andIf(name.isNotEmpty()) { EmployeeSearchSpecification.startsWithFirstName(name) }
                .andIf(surname.isNotEmpty()) { EmployeeSearchSpecification.startsWithLastName(surname) }

        val specClient = // todo implemetirati za last name takodje
            if (name.isNotEmpty()) {
                ClientSearchSpecification.firstNameContains(name)
            } else {
                Specification.where(ClientSearchSpecification.firstNameContains(""))
            }

        val agentsAndClients = mutableListOf<ActuaryResponseDto>()

        val agents = employeeRepository.findAll(finalEmployeeSpec)
        for (employee in agents) {
            val actuaryDto = employee.toActuaryDto()
            if (actuaryDto != null) {
                agentsAndClients.add(actuaryDto)
            }
        }

        val clients = clientRepository.findAll(specClient)
        for (client in clients) {
            val actuaryDto =
                ActuaryResponseDto(
                    client.id!!,
                    client.firstName!!,
                    client.lastName!!,
                    client.role?.name!!,
                    BigDecimal.ZERO,
                )
            agentsAndClients.add(actuaryDto)
        }

        return agentsAndClients.right()
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

    fun updateUsedLimit(id: Long, newLimit: BigDecimal): Either<ActuaryServiceError, ActuaryLimitResponseDto> {
        val actuaryLimit = actuaryLimitRepository.findByEmployeeId(id).orElse(null)
            ?: return ActuaryServiceError.ActuaryLimitNotFound(id).left()

        actuaryLimit.usedLimit = newLimit
        actuaryLimitRepository.save(actuaryLimit)

        return ActuaryLimitResponseDto(
            limitAmount = actuaryLimit.limitAmount,
            usedLimit = actuaryLimit.usedLimit,
            needsApproval = actuaryLimit.needsApproval
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
