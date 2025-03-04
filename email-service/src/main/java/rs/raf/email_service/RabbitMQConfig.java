package rs.raf.email_service;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue resetPasswordQueue() {
        return new Queue("reset-password", false);
    }

    @Bean
    public Queue setPasswordQueue() {
        return new Queue("set-password", false);
    }

    @Bean
    public Queue activateClientAccountQueue() {
        return new Queue("activate-client-account", false);
    }
}