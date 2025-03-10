package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.domain.entity.Currency;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(Currency fromCurrency, Currency toCurrency);

}
