package pack.userservicekotlin.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import pack.userservicekotlin.domain.entities.Employee
import java.util.*

object EmployeeSearchSpecification {
    // Pretraga po imenu, koristi startsWith
    fun startsWithFirstName(firstName: String?): Specification<Employee?> =
        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (firstName == null) {
                null
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("firstName")),
                    firstName.lowercase(Locale.getDefault()) + "%",
                )
            }
        }

    // Pretraga po prezimenu, koristi startsWith
    fun startsWithLastName(lastName: String?): Specification<Employee?> =
        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (lastName == null) {
                null
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("lastName")),
                    lastName.lowercase(Locale.getDefault()) + "%",
                )
            }
        }

    // Pretraga po email-u
    fun startsWithEmail(email: String?): Specification<Employee?> =
        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (email == null) {
                null
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    email.lowercase(Locale.getDefault()) + "%",
                )
            }
        }

    // Pretraga po poziciji
    fun startsWithPosition(position: String?): Specification<Employee?> =
        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            if (position == null) {
                null
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("position")),
                    position.lowercase(Locale.getDefault()) + "%",
                )
            }
        }

    // Pretraga po role-u
    fun hasRole(roleName: String?): Specification<Employee?> =
        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, cb: CriteriaBuilder ->
            if (roleName == null) {
                null
            } else {
                cb.equal(
                    cb.lower(root.join<Any, Any>("role").get("name")),
                    roleName.lowercase(Locale.getDefault()),
                )
            }
        }

//    // Pretraga po role-u
//    fun hasRole(roleName: String): Specification<Employee?> =
//        Specification { root: Root<Employee?>, query: CriteriaQuery<*>?, cb: CriteriaBuilder ->
//            cb.equal(
//                cb.lower(root.join<Any, Any>("role").get("name")),
//                roleName.lowercase(Locale.getDefault()),
//            )
//        }

    inline fun <T> Specification<T>.andIf(
        condition: Boolean,
        specProvider: () -> Specification<T>,
    ): Specification<T> = if (condition) this.and(specProvider()) else this
}
