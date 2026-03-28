package com.example.edi.insuranceresponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.edi.insuranceresponse", "com.example.edi.common"})
public class InsuranceResponseApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsuranceResponseApplication.class, args);
    }
}
