package com.example.edi.priorauth;

import com.example.edi.priorauth.config.InterchangeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.example.edi.priorauth", "com.example.edi.common"})
@EnableConfigurationProperties(InterchangeProperties.class)
public class PriorAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriorAuthApplication.class, args);
    }
}
