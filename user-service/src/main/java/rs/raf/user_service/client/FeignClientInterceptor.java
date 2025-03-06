package rs.raf.user_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/// Ova komponenta sluzi da u sve Feign pozive ubaci token koji nam treba da bi mogli da radimo autorizaciju za komunikaciju izmedju servisa trenutno radi testiranja
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    // Dugotrajni token ovde
    private static final String STATIC_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicGVybWlzc2lvbnMiOlsiZW1wbG95ZWUiLCJhZG1pbiJdLCJ1c2VySWQiOjMsImlhdCI6MTc0MTA5NzA2MywiZXhwIjoyNjA1MDk3MDYzfQ.4myWYAgdkvHCMPs4wt_lBUfe2RzWHzHRXUOxxCN2FgriQlvsHJ6WktsmLhZrzYP4COTK05m-1fgsxPus_PKlNA";


    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + STATIC_TOKEN);
    }
}
