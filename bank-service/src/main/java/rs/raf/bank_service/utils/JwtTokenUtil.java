package rs.raf.bank_service.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtTokenUtil {

    private static final Key secret = Keys.hmacShaKeyFor("si-2024-banka-3-tajni-kljuc-za-jwt-generisanje-tokena-mora-biti-512-bitova-valjda-je-dovoljno".getBytes());
    private final long expiration = 86400000;

    public String getSubjectFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid or missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "").trim();

        // Parsiramo token i vadimo userId
        return getClaimsFromToken(token).get("userId", Long.class);
    }

    public String getUserRoleFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Invalid or missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "").trim();

        // Parsiramo token i vadimo userId
        return getClaimsFromToken(token).get("role", String.class);
    }
}
