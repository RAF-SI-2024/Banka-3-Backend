package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service

public class PaymentCallbackService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentCallbackService(RestTemplate restTemplate, @Value("${spring.cloud.openfeign.client.config.stock-service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void notifySuccess(String url, Long callbackId) {
        try {
            HttpEntity<Long> request = createRequestEntity(callbackId);
            restTemplate.postForEntity(baseUrl + url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to notify successful payment to url {} with callbackId {}, error: {}", baseUrl + url, callbackId, e.getMessage());
        }
    }

    public void notifyFailure(String url, Long callbackId) {
        try {
            HttpEntity<Long> request = createRequestEntity(callbackId);
            restTemplate.postForEntity(baseUrl + url, request, Void.class);
        } catch (Exception e) {
            log.error("Failed to notify failed payment to url {} with callbackId {}, error: {}", baseUrl + url, callbackId, e.getMessage());
        }
    }

    private HttpEntity<Long> createRequestEntity(Long callbackId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwidXNlcklkIjozLCJpYXQiOjE3NDE1MjEwMTEsImV4cCI6MjA1NzA1MzgxMX0.3425U9QrOg04G_bZv8leJNYEOKy7C851P5pWv0k9R3rWpA0ePoeBGpLDd-vKK2qNVgi-Eu2PkfFz41WdUTdFeQ");
        return new HttpEntity<>(callbackId, headers);
    }
}