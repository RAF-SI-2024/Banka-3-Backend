package rs.raf.bank_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanType;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class LoanRequestSpecification {

    public static Specification<LoanRequest> filterBy(LoanType type, String accountNumber) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtriranje po LoanType ako nije null
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // Filtriranje po accountNumber ako nije null ili prazan string
            if (accountNumber != null && !accountNumber.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("accountNumber"), accountNumber));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
