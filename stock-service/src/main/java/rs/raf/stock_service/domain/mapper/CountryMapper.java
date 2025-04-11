package rs.raf.stock_service.domain.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.CountryDto;
import rs.raf.stock_service.domain.entity.Country;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CountryMapper {

    private final HolidayMapper holidayMapper;

    public CountryDto toDto(Country country){
        if (country == null) return null;

        return new CountryDto(
                country.getId(),
                country.getName(),
                country.getOpenTime(),
                country.getCloseTime(),
                country.getHolidays() == null ? null :
                        country.getHolidays().stream().map(holidayMapper::toDto).collect(Collectors.toList())
        );
    }
}
