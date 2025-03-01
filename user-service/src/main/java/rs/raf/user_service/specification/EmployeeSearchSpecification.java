package rs.raf.user_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.entity.Employee;

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
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), email.toLowerCase());
    }

    // Pretraga po poziciji
    public static Specification<Employee> startsWithPosition(String position) {
        return (root, query, criteriaBuilder) -> position == null ? null :
                criteriaBuilder.like(criteriaBuilder.lower(root.get("position")), position.toLowerCase());
    }
}


