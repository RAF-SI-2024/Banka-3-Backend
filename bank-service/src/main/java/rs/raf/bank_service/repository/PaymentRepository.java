package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    /// Za posle dodati
}
