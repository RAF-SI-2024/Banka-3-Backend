package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.CardRequest;

public interface CardRequestRepository extends JpaRepository<CardRequest, Long> {
}
