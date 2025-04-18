package rs.raf.bank_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rs.raf.bank_service.client.StockClient;
import rs.raf.bank_service.domain.dto.TrackedPaymentNotifyDto;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentCallbackService {
    private final StockClient stockClient;

    public void notifySuccess(Long callbackId) {
        try {
            stockClient.notifySuccess(new TrackedPaymentNotifyDto(callbackId));
        } catch (Exception e) {
            log.error("Failed to notify successful payment with callbackId {}, error: {}", callbackId, e.getMessage());
        }
    }

    public void notifyFailure(Long callbackId) {
        try {
            stockClient.notifyFail(new TrackedPaymentNotifyDto(callbackId));
        } catch (Exception e) {
            log.error("Failed to notify failed payment with callbackId {}, error: {}", callbackId, e.getMessage());
        }
    }
}