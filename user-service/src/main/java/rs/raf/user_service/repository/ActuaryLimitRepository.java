package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.domain.entity.ActuaryLimit;

import java.util.Optional;

public interface ActuaryLimitRepository extends JpaRepository<ActuaryLimit, Long> {
    Optional<ActuaryLimit> findByEmployeeId(Long employeeId);

}
