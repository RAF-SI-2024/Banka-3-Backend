package pack.userservicekotlin.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.springframework.stereotype.Service
import pack.userservicekotlin.arrow.CompanyServiceError
import pack.userservicekotlin.domain.dto.company.CompanyResponseDto
import pack.userservicekotlin.domain.dto.company.CreateCompanyDto
import pack.userservicekotlin.domain.entities.Company
import pack.userservicekotlin.domain.mapper.toDto
import pack.userservicekotlin.repository.ActivityCodeRepository
import pack.userservicekotlin.repository.ClientRepository
import pack.userservicekotlin.repository.CompanyRepository

@Service
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val clientRepository: ClientRepository,
    private val activityCodeRepository: ActivityCodeRepository,
) {
    fun createCompany(createCompanyDto: CreateCompanyDto): Either<CompanyServiceError, CompanyResponseDto> {
        val ownerId = createCompanyDto.majorityOwner!!
        val activityCodeId = createCompanyDto.activityCode!!

        val client =
            clientRepository.findById(ownerId).orElse(null)
                ?: return CompanyServiceError.OwnerNotFound(ownerId).left()

        val activityCode =
            activityCodeRepository.findById(activityCodeId).orElse(null)
                ?: return CompanyServiceError.ActivityCodeNotFound(activityCodeId).left()

        // Uncomment these when validations are in place
//
//        companyRepository.findByRegistrationNumber(createCompanyDto.registrationNumber!!)?.let {
//            return CompanyServiceError.RegistrationNumberExists(createCompanyDto.registrationNumber).left()
//        }
//
//        companyRepository.findByTaxId(createCompanyDto.taxId!!)?.let {
//            return CompanyServiceError.TaxIdExists(createCompanyDto.taxId).left()
//        }

        val company =
            Company(
                name = createCompanyDto.name,
                registrationNumber = createCompanyDto.registrationNumber,
                taxId = createCompanyDto.taxId,
                activityCode = activityCode.id,
                address = createCompanyDto.address,
                majorityOwner = client,
            )

        return companyRepository.save(company).toDto()!!.right()
    }

    fun getCompanyById(id: Long): Either<CompanyServiceError, CompanyResponseDto> =
        companyRepository
            .findById(id)
            .map { it.toDto()!! }
            .orElse(null)
            ?.right()
            ?: CompanyServiceError.CompanyNotFound(id).left()

    fun getCompaniesForClientId(id: Long): List<CompanyResponseDto> = companyRepository.findByMajorityOwnerId(id).map { it.toDto()!! }
}
