package pack.userservicekotlin.domain

import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.domain.dto.client.CreateClientDto
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.domain.dto.permission.PermissionRequestDto
import pack.userservicekotlin.domain.dto.role.RoleRequestDto
import java.text.SimpleDateFormat
import java.time.LocalDate

object TestRequestData {
    fun validCreateEmployeeDto() =
        CreateEmployeeDto(
            firstName = "John",
            lastName = "Doe",
            birthDate = SimpleDateFormat("yyyy-MM-dd").parse("1990-01-01"),
            gender = "M",
            email = "john.doe@example.com",
            active = true,
            phone = "0612345678",
            address = "Main Street 1",
            username = "johndoe",
            position = "Developer",
            department = "Engineering",
            jmbg = "1234567890123",
            role = "EMPLOYEE",
        )

    fun validUpdateEmployeeDto() =
        UpdateEmployeeDto(
            lastName = "Doe",
            gender = "M",
            phone = "0612345678",
            address = "Main Street 1",
            position = "Senior Dev",
            department = "Engineering",
            role = "EMPLOYEE",
        )

    fun validCreateClientDto() =
        CreateClientDto(
            firstName = "Jane",
            lastName = "Doe",
            birthDate = SimpleDateFormat("yyyy-MM-dd").parse("1992-05-15"),
            gender = "F",
            email = "jane.doe@example.com",
            phone = "0612345678",
            address = "Client Street 2",
            username = "janedoe",
            jmbg = "3210987654321",
        )

    fun validCreateCompanyDto() =
        CreateCompanyDto(
            name = "Test Corp",
            registrationNumber = "12345678",
            taxId = "87654321",
            activityCode = "6201",
            address = "Corporate Blvd 99",
            majorityOwnerId = 1L,
        )

    fun validRoleRequestDto() =
        RoleRequestDto(
            id = 1L,
            name = "ADMIN",
        )

    fun validPermissionRequestDto() =
        PermissionRequestDto(
            id = 1L,
            name = "MANAGE_USERS",
        )

    fun validAuthorizedPersonnelDto() =
        CreateAuthorizedPersonnelDto(
            firstName = "Alex",
            lastName = "Smith",
            dateOfBirth = LocalDate.of(1985, 7, 20),
            gender = "M",
            email = "alex.smith@corp.com",
            phoneNumber = "0654321987",
            address = "Authorized Lane 12",
            companyId = 1L,
        )
}
