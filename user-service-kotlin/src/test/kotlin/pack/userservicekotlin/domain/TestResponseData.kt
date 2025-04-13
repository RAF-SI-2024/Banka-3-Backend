package pack.userservicekotlin.domain

import pack.userservicekotlin.domain.dto.authorized_presonnel.AuthorizedPersonnelResponseDto
import pack.userservicekotlin.domain.dto.client.ClientResponseDto
import pack.userservicekotlin.domain.dto.company.CompanyResponseDto
import pack.userservicekotlin.domain.dto.employee.EmployeeResponseDto
import pack.userservicekotlin.domain.dto.permission.PermissionResponseDto
import pack.userservicekotlin.domain.dto.role.RoleResponseDto
import pack.userservicekotlin.domain.dto.verification.VerificationResponseDto
import pack.userservicekotlin.domain.enums.VerificationStatus
import pack.userservicekotlin.domain.enums.VerificationType
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime

object TestResponseData {
    fun validEmployeeResponseDto() =
        EmployeeResponseDto(
            id = 1L,
            username = "johndoe",
            position = "Developer",
            department = "Engineering",
            active = true,
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            jmbg = "1234567890123",
            birthDate = Date.valueOf("1990-01-01"),
            gender = "M",
            phone = "0612345678",
            address = "Main Street 1",
            role = "EMPLOYEE",
        )

    fun validClientResponseDto() =
        ClientResponseDto(
            id = 1L,
            firstName = "Jane",
            lastName = "Doe",
            email = "jane.doe@example.com",
            address = "Client Avenue 5",
            phone = "061111222",
            gender = "F",
            birthDate = Date.valueOf("1992-05-15"),
            jmbg = "9876543210123",
            username = "janedoe",
        )

    fun validCompanyResponseDto() =
        CompanyResponseDto(
            id = 1L,
            name = "Tech Corp",
            registrationNumber = "12345678",
            taxId = "87654321",
            activityCode = "6201",
            address = "Corporate St 12",
            majorityOwnerId = 1L,
        )

    fun validAuthorizedPersonnelResponseDto() =
        AuthorizedPersonnelResponseDto(
            id = 1L,
            firstName = "Alex",
            lastName = "Smith",
            dateOfBirth = LocalDate.of(1985, 7, 20),
            gender = "M",
            email = "alex.smith@corp.com",
            phoneNumber = "0654321987",
            address = "Authorized Lane 12",
            companyId = 1L,
        )

    fun validPermissionResponseDto() =
        PermissionResponseDto(
            id = 1L,
            name = "MANAGE_USERS",
        )

    fun validRoleResponseDto() =
        RoleResponseDto(
            id = 1L,
            name = "ADMIN",
            permissions = setOf(validPermissionResponseDto()),
        )

    fun validVerificationResponseDto() =
        VerificationResponseDto(
            userId = 1L,
            expirationTime = LocalDateTime.now().plusDays(1),
            targetId = 100L,
            status = VerificationStatus.PENDING,
            verificationType = VerificationType.LOGIN,
            createdAt = LocalDateTime.now(),
        )
}
