package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.raf.user_service.entity.Employee;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    // Check if an employee exists with the given username
    boolean existsByUsername(String username);

    // Check if an employee exists with the given email
    boolean existsByEmail(String email);

    Optional<Employee> findByEmail(String email);
}
