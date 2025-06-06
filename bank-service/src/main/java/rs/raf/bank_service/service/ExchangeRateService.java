package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.ExchangeRateClient;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.CurrencyDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.dto.UpdateExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.domain.mapper.ExchangeRateMapper;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateClient exchangeRateClient;

    public void updateExchangeRates() {
        UpdateExchangeRateDto response = null;

        // sve ide preko RSD, tjs ako hocemo EUR -> USD, moramo EUR -> RSD -> USD
        // tako da nam ne trebaju sve konverzije, samo sa RSD

        Currency fromCurrency = currencyRepository.findByCode("RSD").orElse(null);

        try {
            response = exchangeRateClient.getExchangeRates(fromCurrency.getCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (response == null || response.getConversionRates() == null || !response.getResult().equals("success")) {
            return;
        }

        Map<String, BigDecimal> conversionRates = response.getConversionRates();

        for (String currencyCode : conversionRates.keySet()) {
            //Ne znam da li treba ili ne treba da se cuva ExchangeRate Valute u istu Valutu - ako treba izbrisati
            if (currencyCode.equals(fromCurrency.getCode()))
                continue;

            Currency toCurrency = currencyRepository.findByCode(currencyCode).orElse(null);

            if (toCurrency == null)
                continue;

            ExchangeRate exchangeRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency)
                    .orElse(null);

            if (exchangeRate == null) {
                exchangeRate = new ExchangeRate();
                exchangeRate.setFromCurrency(fromCurrency);
                exchangeRate.setToCurrency(toCurrency);
            }

            BigDecimal rate = conversionRates.get(currencyCode);
            exchangeRate.setExchangeRate(rate);

            exchangeRate.setSellRate(rate.multiply(new BigDecimal("1.01")));
            exchangeRateRepository.save(exchangeRate);

            ExchangeRate mirrored = ExchangeRate.builder()
                    .fromCurrency(exchangeRate.getToCurrency())
                    .toCurrency(exchangeRate.getFromCurrency())
                    .exchangeRate(BigDecimal.ONE.divide(exchangeRate.getExchangeRate(), 6, RoundingMode.UP))
                    .sellRate(BigDecimal.ONE.divide(
                                    exchangeRate.getExchangeRate(), 6, RoundingMode.UP)
                            .multiply(new BigDecimal("1.01")))
                    .build();

            exchangeRateRepository.save(mirrored);
        }


    }

//    @Scheduled(cron = "0 0 8 * * ?")
    public void updateExchangeRatesDaily() {
        updateExchangeRates();
    }

    public List<ExchangeRateDto> getExchangeRates() {
        /*return exchangeRateRepository.findAll().stream().map(exchangeRate ->
                ExchangeRateMapper.toDto(exchangeRate)).collect(Collectors.toList());*/

        //izmena da vraca samo kursnu listu aktivnih valuta - vratiti na gornje ako treba da se vracaju i neaktivne valute
        return exchangeRateRepository.findAllActiveExchangeRates().stream().map(ExchangeRateMapper::toDto)
                .collect(Collectors.toList());

    }


    public BigDecimal convert(ConvertDto convertDto) {


        ExchangeRateDto exchangeRateDto = getExchangeRate(convertDto.getFromCurrencyCode(), convertDto.getToCurrencyCode());


        return convertDto.getAmount().multiply(exchangeRateDto.getExchangeRate());

    }


    public ExchangeRateDto getExchangeRate(String fromCurrencyCode, String toCurrencyCode) {
        Currency fromCurrency = currencyRepository.findByCode(fromCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(fromCurrencyCode));
        Currency toCurrency = currencyRepository.findByCode(toCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException(toCurrencyCode));

        Optional<ExchangeRate> directRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        if (directRate.isPresent()) {
            ExchangeRate rate = directRate.get();
            return new ExchangeRateDto(
                    new CurrencyDto(fromCurrency.getCode(), fromCurrency.getName(), fromCurrency.getSymbol()),
                    new CurrencyDto(toCurrency.getCode(), toCurrency.getName(), toCurrency.getSymbol()),
                    rate.getExchangeRate(),
                    rate.getSellRate()
            );
        }

        // Ako nema direktnog kursa, koristi RSD kao međukorak
        Currency rsd = currencyRepository.findByCode("RSD")
                .orElseThrow(() -> new CurrencyNotFoundException("RSD"));

        Optional<ExchangeRate> toRsdRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency, rsd);
        Optional<ExchangeRate> fromRsdRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(rsd, toCurrency);

        if (toRsdRate.isPresent() && fromRsdRate.isPresent()) {
            BigDecimal intermediateRate = toRsdRate.get().getExchangeRate().multiply(fromRsdRate.get().getExchangeRate());
            BigDecimal intermediateSellRate = toRsdRate.get().getSellRate().multiply(fromRsdRate.get().getSellRate());
            return new ExchangeRateDto(
                    new CurrencyDto(fromCurrency.getCode(), fromCurrency.getName(), fromCurrency.getSymbol()),
                    new CurrencyDto(toCurrency.getCode(), toCurrency.getName(), toCurrency.getSymbol()),
                    intermediateRate,
                    intermediateSellRate
            );
        }

        throw new ExchangeRateNotFoundException(fromCurrencyCode, toCurrencyCode);
    }
}


