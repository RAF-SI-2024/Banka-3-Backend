package rs.raf.stock_service.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;
import java.util.List;

@AllArgsConstructor
@Getter
public class CountryDto {
    private Long id;
    private String name;
    private LocalTime openTime;
    private LocalTime closeTime;
    private List<HolidayDto> holidays;
}
