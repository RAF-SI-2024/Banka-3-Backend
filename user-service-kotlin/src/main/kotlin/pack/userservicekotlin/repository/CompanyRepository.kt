package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.Company
import java.util.*

@Repository
interface CompanyRepository : JpaRepository<Company, Long> {
    fun findByRegistrationNumber(registrationNumber: String): Optional<Company>

    // bio kao _Id
    fun findByMajorityOwnerId(id: Long): List<Company>

    fun findByTaxId(taxId: String): Optional<Company>
}
