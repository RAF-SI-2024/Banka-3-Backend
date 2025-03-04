package rs.raf.bank_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
<<<<<<< HEAD
=======

>>>>>>> upstream/main
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.stream.Collectors;


@Component
@NoArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Key secret = Keys.hmacShaKeyFor("si-2024-banka-3-tajni-kljuc-za-jwt-generisanje-tokena-mora-biti-512-bitova-valjda-je-dovoljno".getBytes());
<<<<<<< HEAD
=======

>>>>>>> upstream/main
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = getJwtFromRequest(request);
        if (token != null && validateToken(token)) {
            Claims claims = getClaimsFromToken(token);
            String email = claims.getSubject();

            List<String> permissions = claims.get("permissions", List.class);
            List<GrantedAuthority> authorities = permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", authorities);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
<<<<<<< HEAD
              System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
=======
            System.out.println("Authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
>>>>>>> upstream/main
        }

        filterChain.doFilter(request, response);
    }


    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
<<<<<<< HEAD
=======

>>>>>>> upstream/main
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
<<<<<<< HEAD
=======

>>>>>>> upstream/main
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
