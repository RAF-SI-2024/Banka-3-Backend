package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.domain.entity.VerificationRequest;
import rs.raf.user_service.domain.enums.VerificationStatus;

import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    /*
    Optional<VerificationRequest> findByUserId(Long userId);
    void deleteByUserId(Long userId);

     */
    Optional<VerificationRequest> findByTargetId(Long targetId);
    List<VerificationRequest> findByUserIdAndStatus(Long userId, VerificationStatus status);

    Optional<VerificationRequest> findByTargetIdAndStatus(Long targetId, VerificationStatus status);

}

