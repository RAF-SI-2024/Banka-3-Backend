package rs.raf.bank_service.specification;

import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.PaymentStatus;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<Payment> filterPayments(
            Long clientId,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            PaymentStatus paymentStatus,
            String accountNumber,
            String cardNumber
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtriraj po clientId (senderClientId ili receiverClientId)
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("clientId"), clientId),  // senderClientId
                    criteriaBuilder.equal(root.get("receiverClientId"), clientId)  // receiverClientId
            ));

            // Filtriraj po datumu
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }

            // Filtriraj po iznosu
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // Filtriraj po statusu
            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), paymentStatus));
            }

            // Filtriraj po broju računa
            if (accountNumber != null) {
                predicates.add(criteriaBuilder.equal(root.get("senderAccount").get("accountNumber"), accountNumber));
            }

            // Filtriraj po broju kartice
            if (cardNumber != null) {
                predicates.add(criteriaBuilder.equal(root.get("card").get("cardNumber"), cardNumber));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}