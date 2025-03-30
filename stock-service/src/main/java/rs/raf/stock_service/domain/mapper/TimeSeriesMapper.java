package rs.raf.stock_service.domain.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import rs.raf.stock_service.domain.dto.TimeSeriesDto;
import rs.raf.stock_service.domain.entity.ForexPair;
import rs.raf.stock_service.domain.entity.Listing;

@Component
@AllArgsConstructor
public class TimeSeriesMapper {

    private final ObjectMapper objectMapper;

    public TimeSeriesDto mapJsonToCustomTimeSeries(String jsonResponse, Listing listing) {
        try {
            TimeSeriesDto originalDto = objectMapper.readValue(jsonResponse, TimeSeriesDto.class);

            return toCustomTimeSeriesDto(originalDto, listing);
        } catch (Exception e) {
            throw new RuntimeException("Error mapping JSON to Time Series DTO", e);
        }
    }

    public TimeSeriesDto toCustomTimeSeriesDto(TimeSeriesDto originalDto, Listing listing) {
        TimeSeriesDto customDto = new TimeSeriesDto();

        customDto.setValues(originalDto.getValues());
        customDto.setStatus(originalDto.getStatus());

        TimeSeriesDto.MetaDto customMeta = new TimeSeriesDto.MetaDto();
        customMeta.setSymbol(originalDto.getMeta().getSymbol());
        customMeta.setInterval(originalDto.getMeta().getInterval());
        customMeta.setCurrency(originalDto.getMeta().getCurrency());
        customMeta.setType(originalDto.getMeta().getType());
        customMeta.setExchange(originalDto.getMeta().getExchange());

        if (listing instanceof ForexPair) {
            customMeta.setCurrency_base(((ForexPair)listing).getBaseCurrency());
            customMeta.setCurrency_quote(((ForexPair)listing).getQuoteCurrency());
        }

        customDto.setMeta(customMeta);
        return customDto;
    }
}
