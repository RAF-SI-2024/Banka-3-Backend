package rs.raf.user_service.domain.entity;


import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import rs.raf.user_service.domain.enums.VerificationStatus;
import rs.raf.user_service.domain.enums.VerificationType;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long targetId;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // PENDING, APPROVED, DENIED

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType; // LOGIN, LOAN

    private LocalDateTime expirationTime;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String details;
}
