package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.bank_service.domain.entity.ChangeLimitRequest;
import rs.raf.bank_service.domain.enums.VerificationStatus;

import java.util.Optional;

@Repository
public interface ChangeLimitRequestRepository extends JpaRepository<ChangeLimitRequest, Long> {

    Optional<ChangeLimitRequest> findByAccountIdAndStatus(Long accountId, VerificationStatus status);
}
