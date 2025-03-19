package rs.raf.stock_service.domain.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CountryHolidaysDto {
    private List<CountryHolidayDto> countries;
}
