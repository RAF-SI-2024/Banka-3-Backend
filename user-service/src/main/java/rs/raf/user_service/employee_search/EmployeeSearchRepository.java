package rs.raf.user_service.employee_search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.raf.user_service.entity.Employee;

public interface EmployeeSearchRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
}
