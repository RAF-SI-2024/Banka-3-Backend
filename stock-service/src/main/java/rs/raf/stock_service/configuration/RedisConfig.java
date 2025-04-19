package rs.raf.stock_service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import org.springframework.data.redis.core.RedisTemplate;
import rs.raf.stock_service.domain.dto.ListingDto;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ListingDto> listingRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ListingDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key: String
        template.setKeySerializer(new StringRedisSerializer());

        // Value: ListingDto as JSON with class info
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        Jackson2JsonRedisSerializer<ListingDto> serializer = new Jackson2JsonRedisSerializer<>(ListingDto.class);
        serializer.setObjectMapper(mapper);

        template.setValueSerializer(serializer);
        return template;
    }
}
