package rs.raf.stock_service.service;


import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.entity.Country;
import rs.raf.stock_service.domain.entity.Exchange;
import rs.raf.stock_service.exceptions.ExchangesNotLoadedException;
import rs.raf.stock_service.repository.CountryRepository;
import rs.raf.stock_service.repository.ExchangeRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ExchangeService {

    private ExchangeRepository exchangeRepository;
    private CountryRepository countryRepository;

    public void importExchanges(){
        BufferedReader bufferedReader;
        String line;
        if (exchangeRepository.count()==0){
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new ClassPathResource("exchanges.csv").getInputStream()));
                line = bufferedReader.readLine();

                while ((line = bufferedReader.readLine()) != null) {
                    Exchange exchange = new Exchange();
                    String[] attributes = line.split(",");

                    exchange.setMic(attributes[2]);
                    exchange.setName(attributes[0]);
                    exchange.setAcronym(attributes[1]);
                    if (attributes[3].equalsIgnoreCase("usa")){
                        exchange.setPolity(countryRepository.findByName("United States").get());
                    }else {
                        exchange.setPolity(countryRepository.findByName(attributes[3]).get());
                    }
                    exchange.setCurrencyCode(mapCurrencyToCode(attributes[4]));
                    exchange.setTimeZone(getUtcOffset(attributes[5]));

                    exchangeRepository.save(exchange);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public List<Exchange> getAvailableExchanges(){
        List<Exchange> allExchanges = exchangeRepository.findAll();
        if (allExchanges.isEmpty()){
            throw new ExchangesNotLoadedException();
        }
        List<Exchange> availableExchanges = new ArrayList<>();

        for (Exchange exchange: allExchanges){
            if (exchange.isTestMode()){
                availableExchanges.add(exchange);
                continue;
            }

            Country country = exchange.getPolity();
            long offset = exchange.getTimeZone();

            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            LocalDate today = nowUtc.toLocalDate();
            LocalTime currentTime = nowUtc.toLocalTime().plusHours(offset);

            if (country.getHolidays().contains(today)){
                continue;
            }

            if (currentTime.isAfter(country.getOpenTime()) && currentTime.isBefore(country.getCloseTime())){
                availableExchanges.add(exchange);
            }
        }
        return availableExchanges;
    }


    public void toggleTestMode(){
        List<Exchange> allExchanges = exchangeRepository.findAll();
        if (allExchanges.isEmpty()){
            throw new ExchangesNotLoadedException();
        }
        for (Exchange exchange: allExchanges){
            exchange.setTestMode(!exchange.isTestMode());
            exchangeRepository.save(exchange);
        }
    }



    private long getUtcOffset(String timeZoneId) {
        ZoneId zoneId = ZoneId.of(timeZoneId);
        ZoneOffset offset = zoneId.getRules().getOffset(Instant.now());
        return offset.getTotalSeconds() / 3600;
    }

    private String mapCurrencyToCode(String name){
        return switch (name) {
            case "USD", "United States Dollar" -> "USD";
            case "Euro" -> "EUR";
            case "British Pound Sterling" -> "GBP";
            case "Australian Dollar" -> "AUD";
            case "Swiss Franc" -> "CHF";
            default -> "RSD";
        };
    }
}
