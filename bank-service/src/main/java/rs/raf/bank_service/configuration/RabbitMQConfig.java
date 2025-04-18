package rs.raf.bank_service.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String TRANSACTION_QUEUE = "transaction-queue";
    public static final String INTERBANK_DELAY_QUEUE = "interbank.delay.queue";
    public static final String INTERBANK_PROCESS_QUEUE = "interbank.process.queue";
    public static final String INTERBANK_DL_EXCHANGE = "interbank.dlx";


    @Bean
    public Queue transactionQueue() {
        return new Queue(TRANSACTION_QUEUE, true);
    }

    @Bean
    public Queue interbankDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", INTERBANK_DL_EXCHANGE);
        args.put("x-dead-letter-routing-key", "interbank.process");
        args.put("x-message-ttl", 15 * 60 * 1000); // 15 minuta
        return new Queue(INTERBANK_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue interbankProcessQueue() {
        return new Queue(INTERBANK_PROCESS_QUEUE, true);
    }

    @Bean
    public DirectExchange interbankDeadLetterExchange() {
        return new DirectExchange(INTERBANK_DL_EXCHANGE);
    }

    @Bean
    public Binding interbankDlqBinding() {
        return BindingBuilder.bind(interbankProcessQueue())
                .to(interbankDeadLetterExchange())
                .with("interbank.process");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
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
}