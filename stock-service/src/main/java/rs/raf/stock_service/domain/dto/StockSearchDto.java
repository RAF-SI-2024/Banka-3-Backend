package rs.raf.stock_service.domain.dto;

import lombok.Data;

@Data
public class StockSearchDto {
    private String ticker;
    private String name;
    private String region;
    private String matchScore;
}
