package rs.raf.bank_service.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/// Klasa koja nam sluzi kao dummy kredencijali za autorizaciju postavljen je kao addFilterBefore u SpringSecurityConfig klasi sluzi da bi mogli da testiramo pozive i da saljemo zahteve izmedju servisa
/// SLUZI ISKLJUCIVO ZA TESTIRANJE

@Component
public class DummyAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Postavljamo dummy autentifikaciju samo ako nije već postavljena
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("petar.p@example.com",
                            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicGVybWlzc2lvbnMiOlsiYWRtaW4iXSwidXNlcklkIjo0LCJpYXQiOjE3NDA5NDg1MjEsImV4cCI6MTc0MTAzNDkyMX0.J56e-ukYkcsfPIiFJSG5pE4d_zxFy-Qr6Vq9i8JNghNHwJ14dzNYL7QGTrBLlQ11UNc0aivIhjF_zrojW8kgUg",
                            List.of(new SimpleGrantedAuthority("admin")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
