package rs.raf.bank_service.service;

import org.springframework.stereotype.Service;
import rs.raf.bank_service.domain.dto.CurrencyDto;
import rs.raf.bank_service.domain.entity.Currency;
import rs.raf.bank_service.repository.CurrencyRepository;

@Service
public class CurrencyService {
    private final CurrencyRepository currencyRepository;

    public CurrencyService(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public CurrencyDto createCurrency(CurrencyDto currencyDto) {
        Currency currency = new Currency();
        currency.setActive(currencyDto.isActive());
        currency.setCode(currencyDto.getCode());
        currencyDto.setCountries(currencyDto.getCountries());
        currency.setName(currencyDto.getName());
        currency.setSymbol(currencyDto.getSymbol());
        currency.setDescription(currencyDto.getDescription());
        currencyRepository.save(currency);
        return currencyDto;
    }
}
