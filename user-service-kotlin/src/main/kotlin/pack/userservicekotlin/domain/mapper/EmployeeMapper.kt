package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.actuary_limit.ActuaryResponseDto
import pack.userservicekotlin.domain.dto.employee.AgentDto
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.entities.Employee
import java.math.BigDecimal

fun Employee?.toDto(): EmployeeResponseDto? {
    if (this == null) return null

    return EmployeeResponseDto(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        address = address,
        phone = phone,
        gender = gender,
        birthDate = birthDate,
        position = position,
        department = department,
        active = active,
        username = username,
        jmbg = jmbg,
        role = role?.name,
    )
}

fun CreateEmployeeDto?.toEntity(): Employee? {
    if (this == null) return null

    val employee = Employee()
    employee.firstName = firstName
    employee.lastName = lastName
    employee.birthDate = birthDate
    employee.gender = gender
    employee.email = email
    employee.phone = phone
    employee.address = address
    employee.username = username
    employee.position = position
    employee.department = department
    employee.active = active ?: true
    employee.jmbg = jmbg
    // role will be set later manually
    return employee
}

fun Employee?.toActuaryDto(): ActuaryResponseDto? {
    if (this == null || id == null || firstName == null || lastName == null || role?.name == null) return null

    return ActuaryResponseDto(
        id = id!!,
        firstName = firstName!!,
        lastName = lastName!!,
        role = role?.name!!,
        profit = BigDecimal.ZERO,
    )
}

fun EmployeeResponseDto?.toAgentDto(
    limitAmount: BigDecimal,
    usedLimit: BigDecimal,
    needsApproval: Boolean,
): AgentDto? {
    if (this == null ||
        id == null ||
        email == null ||
        firstName == null ||
        lastName == null ||
        address == null ||
        phone == null ||
        gender == null ||
        birthDate == null ||
        position == null ||
        department == null ||
        username == null ||
        jmbg == null ||
        role == null
    ) {
        return null
    }

    return AgentDto(
        id = id,
        username = username,
        position = position,
        department = department,
        active = active,
        firstName = firstName,
        lastName = lastName,
        email = email,
        jmbg = jmbg,
        birthDate = birthDate,
        gender = gender,
        phone = phone,
        address = address,
        role = role,
        limitAmount = limitAmount,
        usedLimit = usedLimit,
        needsApproval = needsApproval,
    )
}
