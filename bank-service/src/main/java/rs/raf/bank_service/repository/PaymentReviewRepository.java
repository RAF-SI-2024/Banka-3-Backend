package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.Payment;
import rs.raf.bank_service.domain.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentReviewRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByClientIdAndTransactionDateBetweenAndAmountBetweenAndPaymentStatus(
            Long clientId,
            LocalDateTime startDate, LocalDateTime endDate,
            BigDecimal minAmount, BigDecimal maxAmount,
            TransactionStatus paymentStatus
    );
}
