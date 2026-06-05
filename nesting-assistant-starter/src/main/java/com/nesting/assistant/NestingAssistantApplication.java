package com.nesting.assistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 套料软件智能助手启动类
 */
@SpringBootApplication(scanBasePackages = "com.nesting.assistant")
@MapperScan("com.nesting.assistant.domain.repository")
public class NestingAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(NestingAssistantApplication.class, args);
    }
}
