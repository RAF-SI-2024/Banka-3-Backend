package rs.raf.user_service.employee_search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import rs.raf.user_service.entity.Employee;

@Service
public class EmployeeSearchService {

    private final EmployeeSearchRepository employeeRepository;

    public EmployeeSearchService(EmployeeSearchRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public Page<Employee> findAll(String position, String department, Boolean active, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSearchSpecification.hasPosition(position))
                .and(EmployeeSearchSpecification.hasDepartment(department))
                .and(EmployeeSearchSpecification.isActive(active));

        return employeeRepository.findAll(spec, pageable);
    }
}
