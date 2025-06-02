package rs.raf.bank_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/// Ova komponenta sluzi da u sve Feign pozive ubaci token koji nam treba da bi mogli da radimo autorizaciju za komunikaciju izmedju servisa trenutno radi testiranja
public class FeignClientInterceptor implements RequestInterceptor {

    private final String token;

    public FeignClientInterceptor(String token) {
        this.token = token;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + token);
    }
}