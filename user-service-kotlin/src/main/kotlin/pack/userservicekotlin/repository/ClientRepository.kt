package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.Client
import java.util.*

@Repository
interface ClientRepository :
    JpaRepository<Client, Long>,
    JpaSpecificationExecutor<Client> {
    fun findByEmail(email: String): Optional<Client>

    fun findByJmbg(jmbg: String): Optional<Client>
}
