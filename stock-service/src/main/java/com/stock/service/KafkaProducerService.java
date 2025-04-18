package com.stock.service;

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

    public void sendStockTradeEvent(Long userId, String stockSymbol, String tradeType, 
                                  Double quantity, Double price, Double totalAmount) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("stockSymbol", stockSymbol);
        event.put("tradeType", tradeType);
        event.put("quantity", quantity);
        event.put("price", price);
        event.put("totalAmount", totalAmount);
        event.put("timestamp", LocalDateTime.now().toString());
        
        sendEvent("bank.stock.trades", event);
    }

    public void sendPortfolioUpdateEvent(Long userId, Map<String, Object> portfolioDetails) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("portfolioDetails", portfolioDetails);
        event.put("timestamp", LocalDateTime.now().toString());
        
        sendEvent("bank.stock.portfolio", event);
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