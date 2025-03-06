package rs.raf.user_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // Dobijanje tokena iz trenutne autentifikacije
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String token = (String) authentication.getCredentials();
            template.header("Authorization", "Bearer " + token);  // Dodavanje tokena u header
        }
    }
}
