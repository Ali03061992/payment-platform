package com.paymentplatform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.paymentplatform")
@ComponentScan(
    basePackages = "com.paymentplatform",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.paymentplatform\\.shared\\.infrastructure\\.(audit|eventing|outbox)\\..*"
    )
)
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
