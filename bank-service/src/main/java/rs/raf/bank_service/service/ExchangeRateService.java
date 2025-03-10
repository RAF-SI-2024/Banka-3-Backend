package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.raf.bank_service.client.ExchangeRateClient;
import rs.raf.bank_service.domain.dto.AccountDto;
import rs.raf.bank_service.domain.dto.ConvertDto;
import rs.raf.bank_service.domain.dto.ExchangeRateDto;
import rs.raf.bank_service.domain.dto.UpdateExchangeRateDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.domain.entity.ExchangeRate;
import rs.raf.bank_service.domain.mapper.AccountMapper;
import rs.raf.bank_service.domain.mapper.ExchangeRateMapper;
import rs.raf.bank_service.exceptions.CurrencyNotFoundException;
import rs.raf.bank_service.exceptions.ExchangeRateNotFoundException;
import rs.raf.bank_service.repository.CurrencyRepository;
import rs.raf.bank_service.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateClient exchangeRateClient;

    public void updateExchangeRates() {
        List<Currency> currencies = currencyRepository.findAll();

        for (Currency fromCurrency : currencies){
            UpdateExchangeRateDto response = null;

            try{
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

                exchangeRate.setExchangeRate(conversionRates.get(currencyCode));
                exchangeRateRepository.save(exchangeRate);
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
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

    public BigDecimal convert(ConvertDto convertDto){
        String fromCurrencyCode = convertDto.getFromCurrencyCode();
        Currency fromCurrency = currencyRepository.findByCode(fromCurrencyCode).
                orElseThrow(() -> new CurrencyNotFoundException(fromCurrencyCode)
        );

        String toCurrencyCode = convertDto.getToCurrencyCode();
        Currency toCurrency = currencyRepository.findByCode(toCurrencyCode).
                orElseThrow(() -> new CurrencyNotFoundException(toCurrencyCode)
        );

        ExchangeRate exchangeRate = exchangeRateRepository.findByFromCurrencyAndToCurrency(fromCurrency,toCurrency)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromCurrencyCode, toCurrencyCode)
        );

        return convertDto.getAmount().multiply(exchangeRate.getExchangeRate());
    }
}
