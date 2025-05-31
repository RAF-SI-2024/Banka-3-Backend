package rs.raf.bank_service.configuration;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import rs.raf.bank_service.client.FeignClientInterceptor;

public class ExternalClientConfig {

    @Bean
    public RequestInterceptor adminRequestInterceptor() {
        String adminToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjEzMDU5NDAwMjk5MywiaWQiOiJjNmY0NDEzMy0wOGYyLTRhNDMtYmQ2NS05Y2ZiNmIxM2ZhNWIiLCJwZXJtaXNzaW9ucyI6IjQ2MTE2ODYwMTg0MjczODc5MDgiLCJpYXQiOjE3NDQ5ODQxNzMsIm5iZiI6MTc0NDk4NDE3M30.3GgcnlhE5U-bz6odR4ph6pqDILPGsJJ5Zczq-oYHTSY"; // bank 2 token
        return new FeignClientInterceptor(adminToken);
    }
}