package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.VerificationRequest;
import rs.raf.user_service.enums.VerificationStatus;

import java.util.List;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {
    /*
    Optional<VerificationRequest> findByUserId(Long userId);
    void deleteByUserId(Long userId);

     */


    List<VerificationRequest> findByUserIdAndStatus(Long userId, VerificationStatus status);
}

