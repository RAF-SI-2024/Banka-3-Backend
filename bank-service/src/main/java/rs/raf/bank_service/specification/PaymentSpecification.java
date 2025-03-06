package rs.raf.bank_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<Payment> filterPayments(Long clientId, 
                                                        LocalDateTime startDate, LocalDateTime endDate, 
                                                        BigDecimal minAmount, BigDecimal maxAmount, 
                                                        PaymentStatus paymentStatus) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (clientId != null) {
                predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
            }

            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("date"), startDate, endDate));
            }

            if (minAmount != null && maxAmount != null) {
                predicates.add(criteriaBuilder.between(root.get("amount"), minAmount, maxAmount));
            }

            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), paymentStatus));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
