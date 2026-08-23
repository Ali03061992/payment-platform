package com.paymentplatform.organization;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.paymentplatform")
@EnableScheduling
@EnableRabbit
@EnableJpaRepositories(basePackages = "com.paymentplatform")
@EntityScan(basePackages = "com.paymentplatform")
public class OrganizationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrganizationServiceApplication.class, args);
    }
}