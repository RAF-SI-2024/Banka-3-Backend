package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.CreditRequest;
import rs.raf.bank_service.domain.enums.CreditRequestApproval;

import java.util.List;

public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {
    List<CreditRequest> findByApproved(CreditRequestApproval approval);
}
