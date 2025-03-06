package rs.raf.user_service.dto;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerificationRequestDto {
    private Long userId;  // ID korisnika koji zahteva verifikaciju
    private Long targetId;  // ID transakcije (targetId)
}
