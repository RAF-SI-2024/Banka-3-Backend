package rs.raf.user_service.domain.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestConfirmedDto {
    private Long targetId;  // ID transakcije (targetId)

}
