package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.user_service.domain.entity.VerificationRequest;

import java.util.List;
import java.util.Optional;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, Long> {

    @Query("SELECT v FROM VerificationRequest v WHERE v.userId = :userId AND v.status = 'PENDING' AND v.expirationTime > CURRENT_TIMESTAMP ORDER BY v.createdAt DESC")
    List<VerificationRequest> findActiveRequests(@Param("userId") Long userId);

    @Query("SELECT v FROM VerificationRequest v WHERE v.userId = :userId AND ((v.status != 'PENDING') OR v.expirationTime < CURRENT_TIMESTAMP) ORDER BY v.createdAt DESC")
    List<VerificationRequest> findInactiveRequests(@Param("userId") Long userId);

    @Query("SELECT v FROM VerificationRequest v WHERE v.id = :id AND v.userId = :userId AND v.status = 'PENDING' AND v.expirationTime > CURRENT_TIMESTAMP ORDER BY v.createdAt DESC")
    Optional<VerificationRequest> findActiveRequest(@Param("id") Long id, @Param("userId") Long userId);

}

