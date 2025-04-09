package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.AuthorizedPersonel
import pack.userservicekotlin.domain.entities.Company

@Repository
interface AuthorizedPersonnelRepository : JpaRepository<AuthorizedPersonel, Long> {
    fun findByCompany(company: Company): List<AuthorizedPersonel>

    fun findByCompanyId(companyId: Long): List<AuthorizedPersonel>
}
