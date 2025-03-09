package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.domain.entity.AuthToken;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);

}
