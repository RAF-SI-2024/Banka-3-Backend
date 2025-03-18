package rs.raf.stock_service.configuration;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AlphavantageConfig {
    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Bean
    public RequestInterceptor alphavantageRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.query("apikey", apiKey);
        };
    }
}
