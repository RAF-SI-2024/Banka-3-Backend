package rs.raf.user_service.entity;


import lombok.*;
import rs.raf.user_service.enums.VerificationStatus;

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
    private Long transactionId; // Veza ka konkretnoj transakciji

    @Enumerated(EnumType.STRING)
    private VerificationStatus status; // PENDING, APPROVED, DENIED

    private LocalDateTime expirationTime;
}
