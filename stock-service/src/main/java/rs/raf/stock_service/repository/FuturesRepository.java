package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.raf.stock_service.domain.entity.FuturesContract;

import java.util.List;
import java.util.Optional;

public interface FuturesRepository extends JpaRepository<FuturesContract, Long> {
    @Query("SELECT f FROM FuturesContract f")
    List<FuturesContract> findAllFuturesContracts();

    Optional<FuturesContract> findByTicker(String ticker);
}
