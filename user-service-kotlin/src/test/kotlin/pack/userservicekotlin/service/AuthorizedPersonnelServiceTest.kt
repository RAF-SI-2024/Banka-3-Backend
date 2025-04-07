package pack.userservicekotlin.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import pack.userservicekotlin.arrow.AuthorizedPersonnelServiceError
import pack.userservicekotlin.domain.TestDataFactory
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.AuthorizedPersonnelRepository
import pack.userservicekotlin.repository.CompanyRepository
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthorizedPersonnelServiceTest {
    @Mock lateinit var authorizedPersonnelRepository: AuthorizedPersonnelRepository

    @Mock lateinit var companyRepository: CompanyRepository

    @InjectMocks
    lateinit var service: AuthorizedPersonnelService

    @Test
    fun `createAuthorizedPersonnel saves and returns DTO`() {
        val company = TestDataFactory.company(id = 1L)
        val dto =
            CreateAuthorizedPersonnelDto(
                firstName = "Ana",
                lastName = "Ivanovic",
                dateOfBirth = LocalDate.of(1990, 1, 1),
                gender = "F",
                email = "ana@example.com",
                phoneNumber = "123456",
                address = "Main St",
                companyId = 1L,
            )
        val entity = dto.toEntity(company)!!

        `when`(companyRepository.findById(1L)).thenReturn(Optional.of(company))
        `when`(authorizedPersonnelRepository.save(any())).thenReturn(entity)

        val result = service.createAuthorizedPersonnel(dto)

        assertTrue(result.isRight())
        assertEquals("Ana", result.getOrNull()?.firstName)
    }

    @Test
    fun `createAuthorizedPersonnel returns error if company not found`() {
        val dto =
            CreateAuthorizedPersonnelDto(
                firstName = "Ana",
                lastName = "Ivanovic",
                dateOfBirth = LocalDate.of(1990, 1, 1),
                gender = "F",
                email = "ana@example.com",
                phoneNumber = "123456",
                address = "Main St",
                companyId = 99L,
            )

        `when`(companyRepository.findById(99L)).thenReturn(Optional.empty())

        val result = service.createAuthorizedPersonnel(dto)

        assertTrue(result.isLeft())
        assertEquals(AuthorizedPersonnelServiceError.CompanyNotFound(99L), result.swap().orNull())
    }

    @Test
    fun `getAuthorizedPersonnelByCompany returns list if company exists`() {
        val company = TestDataFactory.company(id = 2L)
        val personnel = listOf(TestDataFactory.authorizedPersonnel(company))

        `when`(companyRepository.findById(2L)).thenReturn(Optional.of(company))
        `when`(authorizedPersonnelRepository.findByCompany(company)).thenReturn(personnel)

        val result = service.getAuthorizedPersonnelByCompany(2L)

        assertTrue(result.isRight())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getAuthorizedPersonnelByCompany returns error if company not found`() {
        `when`(companyRepository.findById(3L)).thenReturn(Optional.empty())

        val result = service.getAuthorizedPersonnelByCompany(3L)

        assertTrue(result.isLeft())
        assertEquals(AuthorizedPersonnelServiceError.CompanyNotFound(3L), result.swap().orNull())
    }

    @Test
    fun `getAuthorizedPersonnelById returns DTO if found`() {
        val person = TestDataFactory.authorizedPersonnel(TestDataFactory.company(id = 4L))
        `when`(authorizedPersonnelRepository.findById(10L)).thenReturn(Optional.of(person))

        val result = service.getAuthorizedPersonnelById(10L)

        assertTrue(result.isRight())
        assertEquals(person.firstName, result.getOrNull()?.firstName)
    }

    @Test
    fun `getAuthorizedPersonnelById returns error if not found`() {
        `when`(authorizedPersonnelRepository.findById(99L)).thenReturn(Optional.empty())

        val result = service.getAuthorizedPersonnelById(99L)

        assertTrue(result.isLeft())
        assertEquals(AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(99L), result.swap().orNull())
    }

    @Test
    fun `updateAuthorizedPersonnel updates and saves entity`() {
        val existing = TestDataFactory.authorizedPersonnel(TestDataFactory.company(id = 5L))
        val newCompany = TestDataFactory.company(id = 6L)
        val dto =
            CreateAuthorizedPersonnelDto(
                firstName = "Updated",
                lastName = "Person",
                dateOfBirth = LocalDate.of(1990, 1, 1),
                gender = "M",
                email = "updated@example.com",
                phoneNumber = "7890",
                address = "New Street",
                companyId = 6L,
            )

        `when`(authorizedPersonnelRepository.findById(existing.id!!)).thenReturn(Optional.of(existing))
        `when`(companyRepository.findById(6L)).thenReturn(Optional.of(newCompany))
        `when`(authorizedPersonnelRepository.save(existing)).thenReturn(existing)

        val result = service.updateAuthorizedPersonnel(existing.id!!, dto)

        assertTrue(result.isRight())
        assertEquals("Updated", result.getOrNull()?.firstName)
        assertEquals("New Street", result.getOrNull()?.address)
    }

    @Test
    fun `deleteAuthorizedPersonnel deletes if exists`() {
        `when`(authorizedPersonnelRepository.existsById(7L)).thenReturn(true)

        val result = service.deleteAuthorizedPersonnel(7L)

        assertTrue(result.isRight())
        verify(authorizedPersonnelRepository).deleteById(7L)
    }

    @Test
    fun `deleteAuthorizedPersonnel returns error if not found`() {
        `when`(authorizedPersonnelRepository.existsById(8L)).thenReturn(false)

        val result = service.deleteAuthorizedPersonnel(8L)

        assertTrue(result.isLeft())
        assertEquals(AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(8L), result.swap().orNull())
    }
}
