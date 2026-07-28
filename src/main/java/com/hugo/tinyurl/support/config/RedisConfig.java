package com.hugo.tinyurl.support.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.hugo.tinyurl.domain.entity.ShortUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ShortUrl> shortUrlRedisTemplate(RedisConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        ObjectMapper objectMapper = jsonMapper.rebuild()
            .changeDefaultVisibility(visibility -> visibility
                .withFieldVisibility(Visibility.ANY)
                .withGetterVisibility(Visibility.NONE)
                .withIsGetterVisibility(Visibility.NONE))
            .build();

        RedisTemplate<String, ShortUrl> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(objectMapper, ShortUrl.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
