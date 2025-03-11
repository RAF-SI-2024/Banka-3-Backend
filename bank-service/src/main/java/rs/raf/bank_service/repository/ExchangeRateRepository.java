package rs.raf.bank_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(Currency fromCurrency, Currency toCurrency);

    @Query("SELECT er FROM ExchangeRate er " +
            "JOIN er.fromCurrency fc " +
            "JOIN er.toCurrency tc " +
            "WHERE fc.active = true AND tc.active = true")
    List<ExchangeRate> findAllActiveExchangeRates();
}
