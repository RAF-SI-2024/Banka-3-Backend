package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.LoanRequest;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
}
