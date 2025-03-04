package rs.raf.bank_service.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtTokenUtil {

    private static final Key secret = Keys.hmacShaKeyFor("si-2024-banka-3-tajni-kljuc-za-jwt-generisanje-tokena-mora-biti-512-bitova-valjda-je-dovoljno".getBytes());

    public String extractUserId(String token) {
        return getClaimsFromToken(token).get("userId", String.class);
    }

    private Claims getClaimsFromToken(String token) {
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
}
