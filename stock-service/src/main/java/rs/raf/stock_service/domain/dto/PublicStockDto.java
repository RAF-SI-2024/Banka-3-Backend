package rs.raf.stock_service.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicStockDto {
        private Long portfolioEntryId;
        private String security;
        private String ticker;
        private Integer amount;
        private BigDecimal price;
        private LocalDateTime lastModified;
        private String owner;
}
