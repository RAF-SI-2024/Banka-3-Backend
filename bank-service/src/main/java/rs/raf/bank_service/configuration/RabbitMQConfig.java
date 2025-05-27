package rs.raf.bank_service.configuration;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String TRANSACTION_QUEUE = "transaction-queue";
    public static final String EXTERNAL_DELAY_QUEUE = "external.delay.queue";
    public static final String EXTERNAL_PROCESS_QUEUE = "external.process.queue";
    public static final String EXTERNAL_DL_EXCHANGE = "external.dlx";

    @Bean
    public Queue transactionQueue() {
        return new Queue(TRANSACTION_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);

        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain();

        return factory;
    }

    @Bean
    public Queue externalProcessQueue() {
        return new Queue(EXTERNAL_PROCESS_QUEUE, true);
    }

    @Bean
    public Queue externalDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXTERNAL_DL_EXCHANGE);
        args.put("x-dead-letter-routing-key", "external.process");
        args.put("x-message-ttl", 1 * 60 * 1000); // 1 minuta
        return new Queue(EXTERNAL_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange externalDeadLetterExchange() {
        return new DirectExchange(EXTERNAL_DL_EXCHANGE);
    }

    @Bean
    public Binding interbankDlqBinding() {
        return BindingBuilder.bind(externalProcessQueue())
                .to(externalDeadLetterExchange())
                .with("external.process");
    }
}