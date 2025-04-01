package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.CardRequest;
import rs.raf.bank_service.domain.enums.RequestStatus;

import java.util.List;

public interface CardRequestRepository extends JpaRepository<CardRequest, Long> {
    List<CardRequest> findByAccountNumberAndStatus(String accountNumber, RequestStatus pending);
}
