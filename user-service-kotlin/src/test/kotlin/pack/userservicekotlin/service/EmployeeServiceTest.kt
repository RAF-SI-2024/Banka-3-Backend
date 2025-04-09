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
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.employee.CreateEmployeeDto
import pack.userservicekotlin.domain.dto.employee.UpdateEmployeeDto
import pack.userservicekotlin.domain.dto.external.EmailRequestDto
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
    fun `deleteEmployee removes employee`() {
        val employee = TestDataFactory.employee(id = 11L)
        `when`(employeeRepository.findById(11L)).thenReturn(Optional.of(employee))

        val result = employeeService.deleteEmployee(11L)

        assertTrue(result.isRight())
        verify(employeeRepository).delete(employee)
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
    fun `activateEmployee sets active to true`() {
        val employee = TestDataFactory.employee(id = 13L, active = false)
        `when`(employeeRepository.findById(13L)).thenReturn(Optional.of(employee))
        `when`(employeeRepository.save(employee)).thenReturn(employee)

        val result = employeeService.activateEmployee(13L)

        assertTrue(result.isRight())
        assertTrue(employee.active)
    }

    @Test
    fun `createEmployee saves employee and sends email`() {
        val dto =
            CreateEmployeeDto(
                firstName = "Jane",
                lastName = "Doe",
                email = "jane@example.com",
                gender = "F",
                phone = "999999999",
                address = "Street 12",
                position = "Agent",
                department = "Sales",
                role = "AGENT",
            )
        val role = TestDataFactory.role("AGENT")

        `when`(roleRepository.findByName("AGENT")).thenReturn(Optional.of(role))
        `when`(employeeRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = employeeService.createEmployee(dto)

        assertTrue(result.isRight())
        verify(rabbitTemplate).convertAndSend(eq("set-password"), any<EmailRequestDto>())
        verify(authTokenRepository).save(any())
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
}
