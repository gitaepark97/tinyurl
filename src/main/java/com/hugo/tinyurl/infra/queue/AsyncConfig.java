package com.hugo.tinyurl.infra.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableResilientMethods
class AsyncConfig {

    @Bean
    ThreadPoolTaskExecutor clickEventExecutor(
        @Value("${app.click-event.async.core-pool-size}") int corePoolSize,
        @Value("${app.click-event.async.max-pool-size}") int maxPoolSize,
        @Value("${app.click-event.async.queue-capacity}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("click-event-");
        executor.initialize();
        return executor;
    }

}
