package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.stock_service.domain.entity.OtcOption;

public interface OtcOptionRepository extends JpaRepository<OtcOption, Long> {
}

