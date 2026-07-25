package com.hugo.tinyurl;

import org.springframework.boot.SpringApplication;

public class TestTinyurlApplication {

    public static void main(String[] args) {
        SpringApplication.from(TinyurlApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
