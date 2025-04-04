package rs.raf.stock_service.domain.dto;

import lombok.*;

@Getter
@Setter
@Data
@Builder
@AllArgsConstructor
public class SetPublicAmountDto {

    private Long portfolioEntryId;
    private Integer publicAmount;
}
