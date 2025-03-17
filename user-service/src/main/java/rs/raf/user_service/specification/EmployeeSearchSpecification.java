package rs.raf.user_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.domain.entity.Employee;
import rs.raf.user_service.domain.entity.Role;

import javax.persistence.criteria.Join;

public class EmployeeSearchSpecification {


    // Pretraga po imenu, koristi startsWith
    public static Specification<Employee> startsWithFirstName(String firstName) {
        return (root, query, criteriaBuilder) -> firstName == null ? null :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), firstName.toLowerCase() + "%");
    }

    // Pretraga po prezimenu, koristi startsWith
    public static Specification<Employee> startsWithLastName(String lastName) {
        return (root, query, criteriaBuilder) -> lastName == null ? null :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), lastName.toLowerCase() + "%");
    }

    // Pretraga po email-u
    public static Specification<Employee> startsWithEmail(String email) {
        return (root, query, criteriaBuilder) -> email == null ? null :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), email.toLowerCase() + "%");
    }

    // Pretraga po poziciji
    public static Specification<Employee> startsWithPosition(String position) {
        return (root, query, criteriaBuilder) -> position == null ? null :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("position")), position.toLowerCase() + "%");
    }
    // Pretraga po role-u
    public static Specification<Employee> hasRole(String roleName) {
        return (root, query, cb) -> roleName == null ? null :
                cb.equal(cb.lower(root.join("role").get("name")), roleName.toLowerCase());
    }
}


