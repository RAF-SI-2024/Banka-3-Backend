package rs.raf.bank_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

/// Ova komponenta sluzi da u sve Feign pozive ubaci token koji nam treba da bi mogli da radimo autorizaciju za komunikaciju izmedju servisa trenutno radi testiranja
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    // Dugotrajni token ovde
    // ADMIN TOKEN
    private static final String STATIC_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwidXNlcklkIjozLCJpYXQiOjE3NDE1MjEwMTEsImV4cCI6MjA1NzA1MzgxMX0.3425U9QrOg04G_bZv8leJNYEOKy7C851P5pWv0k9R3rWpA0ePoeBGpLDd-vKK2qNVgi-Eu2PkfFz41WdUTdFeQ";

    //EMPLOYEE TOKEN
//    private static final String STATIC_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqYW5hLmlAZXhhbXBsZS5jb20iLCJyb2xlIjoiRU1QTE9ZRUUiLCJ1c2VySWQiOjQsImlhdCI6MTc0MTU0NjU3MywiZXhwIjoxNzQxNjMyOTczfQ.dFSPgclmtEWk7kcyryw4iSVTQbqTa15iFnCktrMHBTW8hPgAKQ-PgCOQq8Z4h4MXbh8VB0hTok-cO1nUXIwO5Q";

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + STATIC_TOKEN);
    }
}