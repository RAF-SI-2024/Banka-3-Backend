package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import pack.userservicekotlin.arrow.EmployeeServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.*
import java.util.*

@ExtendWith(MockitoExtension::class)
class EmployeeServiceTest {
    @Mock lateinit var employeeRepository: EmployeeRepository

    @Mock lateinit var userRepository: UserRepository

    @Mock lateinit var authTokenRepository: AuthTokenRepository

    @Mock lateinit var rabbitTemplate: RabbitTemplate

    @Mock lateinit var roleRepository: RoleRepository

    @Mock lateinit var actuaryLimitRepository: ActuaryLimitRepository

    @Mock lateinit var pageable: Pageable

    @InjectMocks
    lateinit var employeeService: EmployeeService

    @Test
    fun `findAll returns paginated employee DTOs`() {
        val employees = listOf(TestDataFactory.employee(id = 1L), TestDataFactory.employee(id = 2L))
        `when`(employeeRepository.findAll(any(), eq(pageable))).thenReturn(PageImpl(employees))

        val result = employeeService.findAll("A", "B", "C", "Manager", pageable)

        assertEquals(2, result.content.size)
        assertEquals("Emp1", result.content[0].firstName)
    }

    @Test
    fun `findById returns employee DTO`() {
        val employee = TestDataFactory.employee(id = 10L)
        `when`(employeeRepository.findById(10L)).thenReturn(Optional.of(employee))

        val result = employeeService.findById(10L)

        assertTrue(result.isRight())
        assertEquals(employee.id, result.getOrNull()?.id)
    }

    @Test
    fun `findById returns error if not found`() {
        `when`(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        val result = employeeService.findById(99L)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.NotFound, result.swap().getOrNull())
    }

    @Test
    fun `deleteEmployee removes employee`() {
        val employee = TestDataFactory.employee(id = 11L)
        `when`(employeeRepository.findById(11L)).thenReturn(Optional.of(employee))

        val result = employeeService.deleteEmployee(11L)

        assertTrue(result.isRight())
        verify(employeeRepository).delete(employee)
    }

    @Test
    fun `deleteEmployee returns error if not found`() {
        `when`(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        val result = employeeService.deleteEmployee(99L)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.NotFound, result.swap().getOrNull())
    }

    @Test
    fun `deactivateEmployee sets active to false`() {
        val employee = TestDataFactory.employee(id = 12L, active = true)
        `when`(employeeRepository.findById(12L)).thenReturn(Optional.of(employee))
        `when`(employeeRepository.save(employee)).thenReturn(employee)

        val result = employeeService.deactivateEmployee(12L)

        assertTrue(result.isRight())
        assertFalse(employee.active)
    }

    @Test
    fun `deactivateEmployee returns error if not found`() {
        `when`(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        val result = employeeService.deactivateEmployee(99L)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.NotFound, result.swap().getOrNull())
    }

    @Test
    fun `activateEmployee sets active to true`() {
        val employee = TestDataFactory.employee(id = 13L, active = false)
        `when`(employeeRepository.findById(13L)).thenReturn(Optional.of(employee))
        `when`(employeeRepository.save(employee)).thenReturn(employee)

        val result = employeeService.activateEmployee(13L)

        assertTrue(result.isRight())
        assertTrue(employee.active)
    }

    @Test
    fun `activateEmployee returns error if not found`() {
        `when`(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        val result = employeeService.activateEmployee(99L)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.NotFound, result.swap().getOrNull())
    }

    @Test
    fun `createEmployee creates and saves employee`() {
        val dto = CreateEmployeeDto(
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            username = "anna",
            gender = "M",
            phone = "1234",
            address = "Street 1",
            position = "Developer",
            department = "IT",
            jmbg = "123",
            birthDate = Date(),
            role = "EMPLOYEE"
        )
        val role = TestDataFactory.role("EMPLOYEE")
        val employee = dto.toEntity()!!
        employee.role = role

        `when`(employeeRepository.findByEmail("john@example.com")).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(role))
        `when`(employeeRepository.save(any())).thenReturn(employee)

        val result = employeeService.createEmployee(dto)

        assertTrue(result.isRight())
        assertEquals("John", result.getOrNull()?.firstName)
    }

    @Test
    fun `createEmployee returns error if role not found`() {
        val dto = CreateEmployeeDto(
            firstName = "John",
            lastName = "Doe",
            email = "john@example.com",
            username = "anna",
            gender = "M",
            phone = "1234",
            address = "Street 1",
            position = "Developer",
            department = "IT",
            jmbg = "123",
            birthDate = Date(),
            role = "EMPLOYEE"
        )

        `when`(employeeRepository.findByEmail("john@example.com")).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.empty())

        val result = employeeService.createEmployee(dto)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.RoleNotFound, result.swap().getOrNull())
    }

    @Test
    fun `createEmployee returns error if email already exists`() {
        val dto = CreateEmployeeDto(
            firstName = "John",
            lastName = "Doe",
            email = "existing@example.com",
            username = "anna",
            gender = "M",
            phone = "1234",
            address = "Street 1",
            position = "Developer",
            department = "IT",
            jmbg = "123",
            birthDate = Date(),
            role = "EMPLOYEE"
        )

        `when`(employeeRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(TestDataFactory.employee()))

        val result = employeeService.createEmployee(dto)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.EmailAlreadyExists, result.swap().getOrNull())
    }

    @Test
    fun `updateEmployee updates existing employee`() {
        val employee = TestDataFactory.employee(id = 2L)
        val dto = UpdateEmployeeDto(
            lastName = "Updated",
            phone = "5678",
            address = "New St",
            gender = "F",
            position = "Manager",
            department = "HR",
            role = "EMPLOYEE"
        )
        val role = TestDataFactory.role("EMPLOYEE")
        
        `when`(employeeRepository.findById(2L)).thenReturn(Optional.of(employee))
        `when`(roleRepository.findByName("EMPLOYEE")).thenReturn(Optional.of(role))
        `when`(employeeRepository.save(employee)).thenReturn(employee)

        val result = employeeService.updateEmployee(2L, dto)

        assertTrue(result.isRight())
        assertEquals("Updated", result.getOrNull()?.lastName)
    }

    @Test
    fun `updateEmployee returns error if employee not found`() {
        val dto = UpdateEmployeeDto(
            lastName = "Updated",
            phone = "5678",
            address = "New St",
            gender = "F",
            position = "Manager",
            department = "HR",
        )
        `when`(employeeRepository.findById(99L)).thenReturn(Optional.empty())

        val result = employeeService.updateEmployee(99L, dto)

        assertTrue(result.isLeft())
        assertEquals(EmployeeServiceError.NotFound, result.swap().getOrNull())
    }

    @Test
    fun `updateEmployee promotes employee to AGENT and assigns limit`() {
        val employee = TestDataFactory.employee(id = 15L)
        employee.role = TestDataFactory.role("EMPLOYEE") // Not AGENT initially
        val dto =
            UpdateEmployeeDto(
                lastName = "Updated",
                gender = "M",
                phone = "123456",
                address = "Updated Address",
                position = "Agent",
                department = "UpdatedDept",
                role = "AGENT",
            )
        val role = TestDataFactory.role("AGENT")

        `when`(employeeRepository.findById(15L)).thenReturn(Optional.of(employee))
        `when`(roleRepository.findByName("AGENT")).thenReturn(Optional.of(role))
        `when`(employeeRepository.save(any())).thenReturn(employee)

        val result = employeeService.updateEmployee(15L, dto)

        assertTrue(result.isRight())
        verify(actuaryLimitRepository).save(any())
    }

    @Test
    fun `updateEmployee demotes AGENT and deletes limit`() {
        val agent = TestDataFactory.employee(id = 16L)
        agent.role = TestDataFactory.role("AGENT")
        val limit = TestDataFactory.actuaryLimit()
        val dto =
            UpdateEmployeeDto(
                lastName = "Demoted",
                gender = "F",
                phone = "777777",
                address = "Demoted Address",
                position = "SomethingElse",
                department = "HR",
                role = "ADMIN",
            )
        val newRole = TestDataFactory.role("ADMIN")

        `when`(employeeRepository.findById(16L)).thenReturn(Optional.of(agent))
        `when`(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(newRole))
        `when`(actuaryLimitRepository.findByEmployeeId(16L)).thenReturn(Optional.of(limit))
        `when`(employeeRepository.save(any())).thenReturn(agent)

        val result = employeeService.updateEmployee(16L, dto)

        assertTrue(result.isRight())
        verify(actuaryLimitRepository).delete(limit)
    }

    @Test
    fun `findByEmail returns employee DTO`() {
        val employee = TestDataFactory.employee(id = 20L)
        `when`(employeeRepository.findByEmail(employee.email!!)).thenReturn(Optional.of(employee))

        val result = employeeService.findByEmail(employee.email!!)

        assertTrue(result.isRight())
        assertEquals(employee.email, result.getOrNull()?.email)
    }

    @Test
    fun `listEmployees returns page of employees`() {
        val employees = listOf(TestDataFactory.employee(id = 1L))
        `when`(employeeRepository.findAll(any(), eq(pageable))).thenReturn(PageImpl(employees))

        val result = employeeService.findAll("", "", "", "", pageable)

        assertEquals(1, result.totalElements)
    }
}
