package rs.raf.user_service.employee_search;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.entity.Employee;

public class EmployeeSearchSpecification {

    public static Specification<Employee> hasPosition(String position) {
        return (root, query, criteriaBuilder) -> position == null ? null :
                criteriaBuilder.equal(root.get("position"), position);
    }

    public static Specification<Employee> hasDepartment(String department) {
        return (root, query, criteriaBuilder) -> department == null ? null :
                criteriaBuilder.equal(root.get("department"), department);
    }

    public static Specification<Employee> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> active == null ? null :
                criteriaBuilder.equal(root.get("active"), active);
    }
}
