package rs.raf.bank_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.exceptions.InvalidExchangeRateException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;



    public BigDecimal getExchangeRate(String fromCurrencyCode, String toCurrencyCode) {
        Currency fromCurrency = currencyRepository.findByCode(fromCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCode(toCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(toCurrencyCode));

        // Pokušaj da pronađeš direktan kurs
        Optional<ExchangeRate> directRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);

        if (directRate.isPresent()) {
            return applyCommission(directRate.get().getExchangeRate());  // Dodaj proviziju
        }

        // Ako nema direktnog kursa, koristi RSD kao međustepenu valutu
        Currency rsd = currencyRepository.findByCode("RSD")
                .orElseThrow(() -> new CurrencyNotFoundException("RSD"));

        Optional<ExchangeRate> toRsdRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, rsd);
        Optional<ExchangeRate> fromRsdRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, toCurrency);

        if (toRsdRate.isPresent() && fromRsdRate.isPresent()) {
            BigDecimal intermediateRate = toRsdRate.get().getExchangeRate().multiply(fromRsdRate.get().getExchangeRate());
            return applyCommission(intermediateRate);
        }

        throw new ExchangeRateNotFoundException(fromCurrencyCode, toCurrencyCode);
    }


    private BigDecimal applyCommission(BigDecimal rate) {
        return rate.multiply(BigDecimal.valueOf(0.99));  // Simulacija 1% provizije
    }

    public ExchangeRate saveOrUpdateExchangeRate(String fromCurrencyCode, String toCurrencyCode, BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidExchangeRateException("Exchange rate must be greater than zero.");
        }

        Currency fromCurrency = currencyRepository.findByCode(fromCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCode(toCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(toCurrencyCode));

        Optional<ExchangeRate> existingRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        ExchangeRate exchangeRate = existingRate.orElseGet(() -> new ExchangeRate(null, null, fromCurrency, toCurrency, rate, null, null));
        exchangeRate.setExchangeRate(rate);

        exchangeRateRepository.save(exchangeRate);

        // Ako nije direktna konverzija između RSD, dodaj posredne kurseve
        if (!fromCurrencyCode.equals("RSD") && !toCurrencyCode.equals("RSD")) {
            Currency rsd = currencyRepository.findByCode("RSD")
                    .orElseThrow(() -> new CurrencyNotFoundException("RSD"));

            BigDecimal rateToRsd = rate.multiply(BigDecimal.valueOf(0.99)); // Sa provizijom
            BigDecimal rateFromRsd = BigDecimal.ONE.divide(rateToRsd, 4, RoundingMode.HALF_UP);

            exchangeRateRepository.save(new ExchangeRate(null, null, fromCurrency, rsd, rateToRsd, null, null));
            exchangeRateRepository.save(new ExchangeRate(null, null, rsd, toCurrency, rateFromRsd, null, null));
        }

        return exchangeRate;
    }
}
