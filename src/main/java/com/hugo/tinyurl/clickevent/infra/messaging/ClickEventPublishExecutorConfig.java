package com.hugo.tinyurl.clickevent.infra.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class ClickEventPublishExecutorConfig {

    @Bean
    ThreadPoolTaskExecutor clickEventPublishExecutor(
        @Value("${app.click-event.async.core-pool-size}") int corePoolSize,
        @Value("${app.click-event.async.max-pool-size}") int maxPoolSize,
        @Value("${app.click-event.async.queue-capacity}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("click-event-publish-");
        // redirect 요청의 trace context가 이 executor 스레드로 넘어가도록 명시적으로 전파한다.
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.initialize();
        return executor;
    }

}
