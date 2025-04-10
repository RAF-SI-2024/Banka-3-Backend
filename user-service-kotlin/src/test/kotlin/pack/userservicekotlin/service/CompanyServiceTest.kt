package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import pack.userservicekotlin.arrow.CompanyServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto
import pack.userservicekotlin.domain.entities.ActivityCode
import pack.userservicekotlin.repository.ActivityCodeRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.CompanyRepository
import java.util.*

@ExtendWith(MockitoExtension::class)
class CompanyServiceTest {
    @Mock lateinit var companyRepository: CompanyRepository

    @Mock lateinit var clientRepository: ClientRepository

    @Mock lateinit var activityCodeRepository: ActivityCodeRepository

    @InjectMocks
    lateinit var companyService: CompanyService

    @Test
    fun `createCompany successfully creates a company`() {
        val client = TestDataFactory.client(id = 1L)
        val activityCode = ActivityCode(id = 100L.toString(), description = "9999")
        val dto =
            CreateCompanyDto(
                name = "NewCo",
                registrationNumber = "REG123",
                taxId = "TAX456",
                activityCode = 100L.toString(),
                address = "New Street",
                majorityOwnerId = 1L,
            )

        `when`(clientRepository.findById(1L)).thenReturn(Optional.of(client))
        `when`(activityCodeRepository.findById(100L.toString())).thenReturn(Optional.of(activityCode))
        `when`(companyRepository.save(any())).thenAnswer { it.arguments[0] }

        val result = companyService.createCompany(dto)

        assertTrue(result.isRight())
        assertEquals("NewCo", result.getOrNull()?.name)
        verify(companyRepository).save(any())
    }

    @Test
    fun `createCompany fails with OwnerNotFound`() {
        val dto =
            CreateCompanyDto(
                name = "FailCo",
                registrationNumber = "R1",
                taxId = "T1",
                activityCode = 200L.toString(),
                address = "Nowhere",
                majorityOwnerId = 99L,
            )

        `when`(clientRepository.findById(99L)).thenReturn(Optional.empty())

        val result = companyService.createCompany(dto)

        assertTrue(result.isLeft())
        assertEquals(CompanyServiceError.OwnerNotFound(99L), result.swap().getOrNull())
    }

    @Test
    fun `createCompany fails with ActivityCodeNotFound`() {
        val client = TestDataFactory.client(id = 1L)
        val dto =
            CreateCompanyDto(
                name = "FailCode",
                registrationNumber = "R2",
                taxId = "T2",
                activityCode = 404L.toString(),
                address = "Ghost Town",
                majorityOwnerId = 1L,
            )

        `when`(clientRepository.findById(1L)).thenReturn(Optional.of(client))
        `when`(activityCodeRepository.findById(404L.toString())).thenReturn(Optional.empty())

        val result = companyService.createCompany(dto)

        assertTrue(result.isLeft())
        assertEquals(CompanyServiceError.ActivityCodeNotFound(404L.toString()), result.swap().getOrNull())
    }

    @Test
    fun `getCompanyById returns company DTO`() {
        val company = TestDataFactory.company(id = 10L)
        `when`(companyRepository.findById(10L)).thenReturn(Optional.of(company))

        val result = companyService.getCompanyById(10L)

        assertTrue(result.isRight())
        assertEquals(10L, result.getOrNull()?.id)
    }

    @Test
    fun `getCompanyById returns error if not found`() {
        `when`(companyRepository.findById(999L)).thenReturn(Optional.empty())

        val result = companyService.getCompanyById(999L)

        assertTrue(result.isLeft())
        assertEquals(CompanyServiceError.CompanyNotFound(999L), result.swap().getOrNull())
    }

    @Test
    fun `getCompaniesForClientId returns list of company DTOs`() {
        val companies =
            listOf(
                TestDataFactory.company(id = 1L),
                TestDataFactory.company(id = 2L),
            )
        `when`(companyRepository.findByMajorityOwnerId(1L)).thenReturn(companies)

        val result = companyService.getCompaniesForClientId(1L)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }
}
