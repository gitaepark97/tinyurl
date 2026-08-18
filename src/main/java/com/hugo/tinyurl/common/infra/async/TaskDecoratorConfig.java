package com.hugo.tinyurl.common.infra.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

// TaskDecorator 빈이 있으면 Boot가 자동 구성하는 기본 @Async 실행기(TaskExecutionAutoConfiguration)에
// 그대로 적용된다 - 실행기를 따로 지정하지 않는 모든 @Async(@ApplicationModuleListener 포함)가
// 이 데코레이터로 trace context를 전파받는다.
@Configuration
class TaskDecoratorConfig {

    @Bean
    TaskDecorator taskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }

}
