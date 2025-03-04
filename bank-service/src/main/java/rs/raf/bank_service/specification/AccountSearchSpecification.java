package rs.raf.bank_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.Account;

public class AccountSearchSpecification {

    public static Specification<Account> accountNumberContains(String accountNumber) {
        return (root, query, criteriaBuilder) -> {
            if (accountNumber == null || accountNumber.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(root.get("accountNumber"), "%" + accountNumber + "%");
        };
    }
}
