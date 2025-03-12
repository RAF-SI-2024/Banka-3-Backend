package rs.raf.bank_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class LoanSpecification {
    public static Specification<Loan> filterBy(LoanType type, String accountNumber, LoanStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (accountNumber != null && !accountNumber.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("account").get("accountNumber"), accountNumber));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
