package rs.raf.stock_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.stock_service.domain.entity.Exchange;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, String> {
}
