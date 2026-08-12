package com.hugo.tinyurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableResilientMethods
// @Async를 항상 가장 바깥쪽에 둬서 @Observed가 실제 비동기 실행 스레드의 작업을 span으로 잡게 한다.
@EnableAsync(order = Ordered.HIGHEST_PRECEDENCE)
public class TinyurlApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyurlApplication.class, args);
    }

}
