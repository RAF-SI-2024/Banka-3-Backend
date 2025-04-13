package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.ActuaryLimit
import java.util.*

@Repository
interface ActuaryLimitRepository : JpaRepository<ActuaryLimit, Long> {
    fun findByEmployeeId(employeeId: Long): Optional<ActuaryLimit>
}
