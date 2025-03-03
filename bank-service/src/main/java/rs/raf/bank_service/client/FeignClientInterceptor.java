package rs.raf.bank_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/// Ova komponenta sluzi da u sve Feign pozive ubaci token koji nam treba da bi mogli da radimo autorizaciju za komunikaciju izmedju servisa trenutno radi testiranja
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            String token = authentication.getCredentials().toString();
            template.header("Authorization", "Bearer " + token);
        }
    }
}