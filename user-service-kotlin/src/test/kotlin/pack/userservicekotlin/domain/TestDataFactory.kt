package pack.userservicekotlin.domain

import pack.userservicekotlin.domain.entities.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

object TestDataFactory {
    fun role(name: String = "AGENT"): Role = Role(id = 1L, name = name, permissions = mutableSetOf())

    fun permission(name: String = "READ"): Permission = Permission(id = 1L, name = name)

    fun client(
        id: Long = 2L,
        username: String = "client$id",
        firstName: String = "Client$id",
        lastName: String = "Last$id",
        birthDate: Date = Date(),
        gender: String = "F",
        email: String = "client$id@example.com",
        phone: String = "0987654321",
        address: String = "Client Street $id",
        password: String = "clientPass$id",
        jmbg: String = "111$id",
        role: Role = role("CLIENT"),
        companies: MutableList<Company> = mutableListOf(),
    ): Client =
        Client(companies = companies).apply {
            this.id = id
            this.username = username
            this.firstName = firstName
            this.lastName = lastName
            this.birthDate = birthDate
            this.gender = gender
            this.email = email
            this.phone = phone
            this.address = address
            this.password = password
            this.jmbg = jmbg
            this.role = role
        }

    fun employee(
        id: Long = 1L,
        username: String = "emp$id",
        firstName: String = "Emp$id",
        lastName: String = "Last$id",
        birthDate: Date = Date(),
        gender: String = "M",
        email: String = "emp$id@example.com",
        phone: String = "1234567890",
        address: String = "Street $id",
        password: String = "pass$id",
        jmbg: String = "000$id",
        role: Role = role(),
        position: String = "Manager",
        department: String = "Sales",
        active: Boolean = true,
        actuaryLimit: ActuaryLimit? = null,
    ): Employee =
        Employee(
            position = position,
            department = department,
            active = active,
            actuaryLimit = actuaryLimit,
        ).apply {
            this.id = id
            this.username = username
            this.firstName = firstName
            this.lastName = lastName
            this.birthDate = birthDate
            this.gender = gender
            this.email = email
            this.phone = phone
            this.address = address
            this.password = password
            this.jmbg = jmbg
            this.role = role
        }

    fun actuaryLimit(
        limitAmount: BigDecimal = BigDecimal(10000),
        usedLimit: BigDecimal = BigDecimal(0),
        needsApproval: Boolean = false,
    ): ActuaryLimit =
        ActuaryLimit(
            id = null,
            employee = employee(),
            limitAmount = limitAmount,
            usedLimit = usedLimit,
            needsApproval = needsApproval,
        )

    fun company(
        id: Long = 1L,
        majorityOwner: Client = client(),
        registrationNumber: String = "REG$id",
        taxId: String = "TAX$id",
    ): Company =
        Company(
            id = id,
            name = "Company$id",
            registrationNumber = registrationNumber,
            taxId = taxId,
            activityCode = "1234",
            address = "Company Address $id",
            majorityOwner = majorityOwner,
            authorizedPersonel = mutableListOf(),
        )

    fun authorizedPersonnel(
        company: Company = company(),
        id: Long? = 1L,
        firstName: String = "John",
        lastName: String = "Doe",
        dateOfBirth: LocalDate = LocalDate.of(1990, 1, 1),
        gender: String = "M",
        email: String = "john.doe@example.com",
        phoneNumber: String = "123-456-7890",
        address: String = "123 Main St",
    ): AuthorizedPersonel =
        AuthorizedPersonel(
            id = id,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            email = email,
            phoneNumber = phoneNumber,
            address = address,
            company = company,
        )
}
