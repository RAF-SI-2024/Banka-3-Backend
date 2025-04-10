package pack.userservicekotlin.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import pack.userservicekotlin.domain.entities.Client
import java.util.*

object ClientSearchSpecification {
    // Pretraga po imenu
    fun firstNameContains(firstName: String?): Specification<Client?> {
        return Specification<Client?> { root: Root<Client?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (firstName == null || firstName.isEmpty()) {
                return@Specification criteriaBuilder.conjunction()
            }
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get<String>("firstName")),
                "%" + firstName.lowercase(Locale.getDefault()) + "%",
            )
        }
    }

    // Pretraga po prezimenu
    fun lastNameContains(lastName: String?): Specification<Client?> {
        return Specification<Client?> { root: Root<Client?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (lastName == null || lastName.isEmpty()) {
                return@Specification criteriaBuilder.conjunction()
            }
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get<String>("lastName")),
                "%" + lastName.lowercase(Locale.getDefault()) + "%",
            )
        }
    }

    // Pretrega po mailu
    fun emailContains(email: String?): Specification<Client?> {
        return Specification<Client?> { root: Root<Client?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (email == null || email.isEmpty()) {
                return@Specification criteriaBuilder.conjunction()
            }
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get<String>("email")),
                "%" + email.lowercase(Locale.getDefault()) + "%",
            )
        }
    }
}
