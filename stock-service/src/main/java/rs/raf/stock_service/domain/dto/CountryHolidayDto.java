package rs.raf.stock_service.domain.dto;


import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class CountryHolidayDto {
    private String name;
    private List<String> holidays;
}
