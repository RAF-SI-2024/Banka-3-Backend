package rs.raf.user_service.entity;


import lombok.*;
import rs.raf.user_service.enums.VerificationStatus;
import rs.raf.user_service.enums.VerificationType;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationRequest {

    /*
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String email; // ili e-mail servis? ; mobilna aplk?

    private String code;

    private LocalDateTime expirationTime;

    private int attempts;

     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String email;
    private Long targetId;

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // PENDING, APPROVED, DENIED

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType; // LOGIN, LOAN

    private LocalDateTime expirationTime;
}
