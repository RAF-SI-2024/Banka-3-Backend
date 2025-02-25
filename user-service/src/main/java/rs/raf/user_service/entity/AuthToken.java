package rs.raf.user_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
public class AuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    Long createdAt;
    Long expiresAt;
    String token;
    String type;
    Long userId;

    public AuthToken(Long createdAt, Long expiresAt, String token, String type, Long userId) {
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.token = token;
        this.type = type;
        this.userId = userId;
    }
}
