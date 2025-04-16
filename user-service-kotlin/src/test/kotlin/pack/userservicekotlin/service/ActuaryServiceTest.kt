package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import pack.userservicekotlin.arrow.ActuaryServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.external.StockClient
import pack.userservicekotlin.repository.ActuaryLimitRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.EmployeeRepository
import java.math.BigDecimal
import java.util.*

@ExtendWith(MockitoExtension::class)
class ActuaryServiceTest {
    @Mock
    lateinit var employeeRepository: EmployeeRepository

    @Mock
    lateinit var actuaryLimitRepository: ActuaryLimitRepository

    @Mock
    lateinit var clientRepository: ClientRepository

    @Mock
    lateinit var stockClient: StockClient

    @Mock
    lateinit var pageable: Pageable

    @InjectMocks
    lateinit var actuaryService: ActuaryService

    @Test
    fun `findAll returns page of EmployeeResponseDto`() {
        val employee = TestDataFactory.employee()
        val employeePage = PageImpl(listOf(employee))

        `when`(employeeRepository.findAll(any(), eq(pageable))).thenReturn(employeePage)

        val result = actuaryService.findAll("a", "b", "c", "d", pageable)

        assertEquals(1, result.totalElements)
        assertEquals(employee.firstName, result.content[0].firstName)
    }

    @Test
    fun `changeAgentLimit succeeds`() {
        val employee = TestDataFactory.employee(id = 1L)
        val limit = TestDataFactory.actuaryLimit()
        employee.actuaryLimit = limit

        `when`(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
        `when`(actuaryLimitRepository.findByEmployeeId(1L)).thenReturn(Optional.of(limit))

        val result = actuaryService.changeAgentLimit(1L, BigDecimal(9000))

        assertTrue(result.isRight())
        assertEquals(BigDecimal(9000), limit.limitAmount)
        verify(actuaryLimitRepository).save(limit)
    }

    @Test
    fun `changeAgentLimit fails when employee not found`() {
        `when`(employeeRepository.findById(1L)).thenReturn(Optional.empty())

        val result = actuaryService.changeAgentLimit(1L, BigDecimal(9000))

        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull() is ActuaryServiceError.EmployeeNotFound)
    }

    @Test
    fun `resetDailyLimit sets usedLimit to zero`() {
        val employee = TestDataFactory.employee(id = 2L)
        val limit = TestDataFactory.actuaryLimit(usedLimit = BigDecimal(100))
        employee.actuaryLimit = limit

        `when`(employeeRepository.findById(2L)).thenReturn(Optional.of(employee))
        `when`(actuaryLimitRepository.findByEmployeeId(2L)).thenReturn(Optional.of(limit))

        val result = actuaryService.resetDailyLimit(2L)

        assertTrue(result.isRight())
        assertEquals(BigDecimal.ZERO, limit.usedLimit)
        verify(actuaryLimitRepository).save(limit)
    }

    @Test
    fun `resetDailyLimit fails when employee not found`() {
        `when`(employeeRepository.findById(2L)).thenReturn(Optional.empty())

        val result = actuaryService.resetDailyLimit(2L)

        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull() is ActuaryServiceError.EmployeeNotFound)
    }

    @Test
    fun `setApproval sets needsApproval`() {
        val employee = TestDataFactory.employee(id = 3L)
        val limit = TestDataFactory.actuaryLimit(needsApproval = false)
        employee.actuaryLimit = limit

        `when`(employeeRepository.findById(3L)).thenReturn(Optional.of(employee))
        `when`(actuaryLimitRepository.findByEmployeeId(3L)).thenReturn(Optional.of(limit))

        val result = actuaryService.setApproval(3L, true)

        assertTrue(result.isRight())
        assertTrue(limit.needsApproval)
        verify(actuaryLimitRepository).save(limit)
    }

    @Test
    fun `getAgentLimit returns response DTO`() {
        val limit = TestDataFactory.actuaryLimit()

        `when`(actuaryLimitRepository.findByEmployeeId(4L)).thenReturn(Optional.of(limit))

        val result = actuaryService.getAgentLimit(4L)

        assertTrue(result.isRight())
        val dto = result.getOrNull()
        assertEquals(limit.limitAmount, dto?.limitAmount)
        assertEquals(limit.usedLimit, dto?.usedLimit)
        assertEquals(limit.needsApproval, dto?.needsApproval)
    }

    @Test
    fun `getAgentLimit fails when limit not found`() {
        `when`(actuaryLimitRepository.findByEmployeeId(4L)).thenReturn(Optional.empty())

        val result = actuaryService.getAgentLimit(4L)

        assertTrue(result.isLeft())
        assertTrue(result.swap().getOrNull() is ActuaryServiceError.ActuaryLimitNotFound)
    }

    @Test
    fun `resetDailyLimits resets all used limits`() {
        val limit1 = TestDataFactory.actuaryLimit(usedLimit = BigDecimal(999))
        val limit2 = TestDataFactory.actuaryLimit(usedLimit = BigDecimal(888))

        `when`(actuaryLimitRepository.findAll()).thenReturn(listOf(limit1, limit2))

        actuaryService.resetDailyLimits()

        assertEquals(BigDecimal.ZERO, limit1.usedLimit)
        assertEquals(BigDecimal.ZERO, limit2.usedLimit)
        verify(actuaryLimitRepository).saveAll(listOf(limit1, limit2))
    }

    @Test
    fun `findActuaries returns enriched page`() {
        val employee = TestDataFactory.employee(id = 1L)
        val employeePage = PageImpl(listOf(employee))
        val order = TestDataFactory.orderDto(userId = 1L, profit = BigDecimal(100))

        `when`(employeeRepository.findAll(any(), eq(pageable))).thenReturn(employeePage)
        `when`(stockClient.getAll()).thenReturn(listOf(order))

        val result = actuaryService.findActuaries(pageable)

        assertTrue(result.isRight())
        val page = result.getOrNull()
        assertEquals(1, page?.totalElements)
        assertEquals(BigDecimal(100), page?.content?.get(0)?.profit)
    }

    @Test
    fun `findActuaries fails when stock client throws exception`() {
        val employee = TestDataFactory.employee(id = 1L)
        val employeePage = PageImpl(listOf(employee))

        `when`(employeeRepository.findAll(any(), eq(pageable))).thenReturn(employeePage)
        `when`(stockClient.getAll()).thenThrow(RuntimeException("Stock service down"))

        val result = actuaryService.findActuaries(pageable)

        assertTrue(result.isLeft())
        val error = result.swap().getOrNull()
        assertTrue(error is ActuaryServiceError.ExternalServiceError)
    }
}
