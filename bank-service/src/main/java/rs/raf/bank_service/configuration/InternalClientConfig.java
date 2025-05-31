package rs.raf.bank_service.configuration;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import rs.raf.bank_service.client.FeignClientInterceptor;

public class InternalClientConfig {

    @Bean
    public RequestInterceptor adminRequestInterceptor() {
        String adminToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwidXNlcklkIjozLCJpYXQiOjE3NDE1MjEwMTEsImV4cCI6MjA1NzA1MzgxMX0.3425U9QrOg04G_bZv8leJNYEOKy7C851P5pWv0k9R3rWpA0ePoeBGpLDd-vKK2qNVgi-Eu2PkfFz41WdUTdFeQ"; // admin token
        return new FeignClientInterceptor(adminToken);
    }
}