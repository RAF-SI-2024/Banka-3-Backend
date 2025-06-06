package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.Employee
import java.util.*

@Repository
interface EmployeeRepository :
    JpaRepository<Employee, Long>,
    JpaSpecificationExecutor<Employee> {
    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): Optional<Employee>

    fun findByJmbg(jmbg: String): Optional<Employee>
    fun findByUsername(username: String): Optional<Employee>
}
