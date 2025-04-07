package pack.userservicekotlin.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.BaseUser
import java.util.*

@Repository
interface UserRepository : JpaRepository<BaseUser, Long> {
    fun findByEmail(email: String): Optional<BaseUser>

    override fun findAll(pageable: Pageable): Page<BaseUser>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByJmbg(jmbg: String): Optional<BaseUser>
}
