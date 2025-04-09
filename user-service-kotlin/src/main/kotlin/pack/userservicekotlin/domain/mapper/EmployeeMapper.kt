package pack.userservicekotlin.domain.mapper

import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.entities.Employee

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
