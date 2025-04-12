package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import rs.raf.bank_service.domain.entity.Payment;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByIdAndClientId(Long id, Long clientId);

    @Query("SELECT SUM(p.exchangeProfit) FROM payments p WHERE p.exchangeProfit IS NOT NULL")
    BigDecimal getBankProfitFromExchange();

}