package com.bank.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void sendTransactionEvent(Long userId, String transactionType, Double amount, String description) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("transactionType", transactionType);
        event.put("amount", amount);
        event.put("description", description);
        event.put("timestamp", LocalDateTime.now().toString());
        
        sendEvent("bank.transactions", event);
    }

    public void sendUserActivityEvent(Long userId, String activityType, Map<String, Object> details) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("activityType", activityType);
        event.put("details", details);
        event.put("timestamp", LocalDateTime.now().toString());
        
        sendEvent("bank.user.activity", event);
    }

    public void sendLoanEvent(Long userId, String loanType, Double amount, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("loanType", loanType);
        event.put("amount", amount);
        event.put("status", status);
        event.put("timestamp", LocalDateTime.now().toString());
        
        sendEvent("bank.loans", event);
    }

    private void sendEvent(String topic, Map<String, Object> event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, message);
        } catch (JsonProcessingException e) {
            // Handle serialization error
            e.printStackTrace();
        }
    }
} 