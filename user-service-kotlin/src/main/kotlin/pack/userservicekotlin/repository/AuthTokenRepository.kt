package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.AuthToken
import java.util.*

@Repository
interface AuthTokenRepository : JpaRepository<AuthToken, Long> {
    fun findByToken(token: String): Optional<AuthToken>
}
