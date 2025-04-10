package rs.raf.user_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.domain.entity.Employee;

public class ClientSearchSpecification {

    //Pretraga po imenu
    public static Specification<Client> firstNameContains(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName == null || firstName.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")),
                    "%" + firstName.toLowerCase() + "%"
            );
        };
    }

    //Pretraga po prezimenu
    public static Specification<Client> lastNameContains(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName == null || lastName.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")),
                    "%" + lastName.toLowerCase() + "%"
            );
        };
    }

    //Pretrega po mailu
    public static Specification<Client> emailContains(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    "%" + email.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Client> hasRole(String roleName) {
        return (root, query, cb) -> roleName == null ? null :
                cb.equal(cb.lower(root.join("role").get("name")), roleName.toLowerCase());
    }
}