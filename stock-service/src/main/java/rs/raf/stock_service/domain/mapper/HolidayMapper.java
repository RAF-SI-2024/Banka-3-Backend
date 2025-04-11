package rs.raf.stock_service.domain.mapper;

import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.HolidayDto;
import rs.raf.stock_service.domain.entity.Holiday;

@Component
public class HolidayMapper {

    public HolidayDto toDto(Holiday holiday){
        if (holiday == null) return null;

        return new HolidayDto(
                holiday.getId(),
                holiday.getDate()
        );
    }
}
