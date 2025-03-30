package rs.raf.stock_service.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TimeSeriesDto {
    private MetaDto meta;
    private List<TimeSeriesValueDto> values;
    private String status;

    @Data
    public static class MetaDto {
        private String symbol;
        private String interval;
        private String currency;
        private String exchange;
        private String type;
    }

    @Data
    public static class TimeSeriesValueDto {
        private String datetime;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
    }
}
