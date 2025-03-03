package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.Currency;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    Optional<Currency> findByCode(String code);
}
