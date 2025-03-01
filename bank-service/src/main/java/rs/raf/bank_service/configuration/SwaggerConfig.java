package rs.raf.bank_service.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info().title("Bank Service")
                        .description("Bank Service API")
                        .version("1.0"));
    }
}