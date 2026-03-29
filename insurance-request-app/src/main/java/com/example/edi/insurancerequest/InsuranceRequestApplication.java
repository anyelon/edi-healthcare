package com.example.edi.insurancerequest;

import com.example.edi.insurancerequest.config.InterchangeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"com.example.edi.insurancerequest", "com.example.edi.common"})
@EnableConfigurationProperties(InterchangeProperties.class)
public class InsuranceRequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsuranceRequestApplication.class, args);
    }
}
