package rs.raf.stock_service.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.dto.CountryHolidayDto;
import rs.raf.stock_service.domain.dto.CountryHolidaysDto;
import rs.raf.stock_service.domain.entity.Country;
import rs.raf.stock_service.domain.entity.Holiday;
import rs.raf.stock_service.repository.CountryRepository;
import rs.raf.stock_service.repository.HolidayRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class HolidayService {

    private final CountryRepository countryRepository;
    private final HolidayRepository holidayRepository;
    private final ObjectMapper objectMapper;


    public void importHolidays() {
        CountryHolidaysDto countryHolidaysDto;
        ClassPathResource resource = new ClassPathResource("holidaysUSA.json");

        try {
            countryHolidaysDto = objectMapper.readValue(resource.getInputStream(), CountryHolidaysDto.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading the JSON file", e);
        }
        if (countryHolidaysDto.getCountries() != null) {
            for (CountryHolidayDto countryHolidayDto : countryHolidaysDto.getCountries()) {
                String countryName = countryHolidayDto.getName();
                List<String> holidayDates = countryHolidayDto.getHolidays();

                Optional<Country> countryOptional = countryRepository.findByName(countryName);
                if (countryOptional.isEmpty()) {
                    continue;
                }

                Country country = countryOptional.get();

                if (country.getHolidays().isEmpty()) {
                    for (String dateStr : holidayDates) {
                        LocalDate date = LocalDate.parse(dateStr);
                        Holiday holiday = new Holiday();
                        holiday.setDate(date);
                        holiday.setCountry(country);
                        holidayRepository.save(holiday);
                        country.getHolidays().add(holiday);
                    }
                    countryRepository.save(country);
                }
            }
        }
    }
}
