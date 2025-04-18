package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.stock_service.domain.entity.TrackedPayment;
import rs.raf.stock_service.domain.enums.TrackedPaymentType;

import java.util.Optional;

public interface TrackedPaymentRepository extends JpaRepository<TrackedPayment, Long> {
    Optional<TrackedPayment> findByIdAndType(Long id, TrackedPaymentType type);
}