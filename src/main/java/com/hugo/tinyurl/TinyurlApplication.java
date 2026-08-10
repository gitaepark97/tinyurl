package com.hugo.tinyurl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableResilientMethods
@EnableAsync
public class TinyurlApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyurlApplication.class, args);
    }

}
