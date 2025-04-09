package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.AuthorizedPersonnelServiceError
import pack.userservicekotlin.domain.dto.authorized_presonnel.AuthorizedPersonnelResponseDto
import pack.userservicekotlin.domain.dto.authorized_presonnel.CreateAuthorizedPersonnelDto
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.domain.mapper.toEntity
import pack.userservicekotlin.repository.AuthorizedPersonnelRepository
import pack.userservicekotlin.repository.CompanyRepository

@Service
class AuthorizedPersonnelService(
    private val authorizedPersonnelRepository: AuthorizedPersonnelRepository,
    private val companyRepository: CompanyRepository,
) {
    fun createAuthorizedPersonnel(
        dto: CreateAuthorizedPersonnelDto,
    ): Either<AuthorizedPersonnelServiceError, AuthorizedPersonnelResponseDto> {
        val company =
            companyRepository.findById(dto.companyId!!).orElse(null)
                ?: return AuthorizedPersonnelServiceError.CompanyNotFound(dto.companyId).left()

        var entity = dto.toEntity(company) ?: return AuthorizedPersonnelServiceError.CompanyNotFound(dto.companyId).left()
        entity = authorizedPersonnelRepository.save(entity)
        return entity.toDto()!!.right()
    }

    fun getAuthorizedPersonnelByCompany(companyId: Long): Either<AuthorizedPersonnelServiceError, List<AuthorizedPersonnelResponseDto>> {
        val company =
            companyRepository.findById(companyId).orElse(null)
                ?: return AuthorizedPersonnelServiceError.CompanyNotFound(companyId).left()

        val personnelList = authorizedPersonnelRepository.findByCompany(company)
        return personnelList.map { it.toDto()!! }.right()
    }

    fun getAuthorizedPersonnelById(id: Long): Either<AuthorizedPersonnelServiceError, AuthorizedPersonnelResponseDto> {
        val personnel =
            authorizedPersonnelRepository.findById(id).orElse(null)
                ?: return AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(id).left()

        return personnel.toDto()!!.right()
    }

    fun updateAuthorizedPersonnel(
        id: Long,
        dto: CreateAuthorizedPersonnelDto,
    ): Either<AuthorizedPersonnelServiceError, AuthorizedPersonnelResponseDto> {
        val personnel =
            authorizedPersonnelRepository.findById(id).orElse(null)
                ?: return AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(id).left()

        val company =
            companyRepository.findById(dto.companyId!!).orElse(null)
                ?: return AuthorizedPersonnelServiceError.CompanyNotFound(dto.companyId).left()

        personnel.apply {
            firstName = dto.firstName
            lastName = dto.lastName
            dateOfBirth = dto.dateOfBirth
            gender = dto.gender
            email = dto.email
            phoneNumber = dto.phoneNumber
            address = dto.address
            this.company = company
        }

        val updated = authorizedPersonnelRepository.save(personnel)
        return updated.toDto()!!.right()
    }

    fun deleteAuthorizedPersonnel(id: Long): Either<AuthorizedPersonnelServiceError, Unit> =
        if (!authorizedPersonnelRepository.existsById(id)) {
            AuthorizedPersonnelServiceError.AuthorizedPersonnelNotFound(id).left()
        } else {
            authorizedPersonnelRepository.deleteById(id)
            Unit.right()
        }
}
