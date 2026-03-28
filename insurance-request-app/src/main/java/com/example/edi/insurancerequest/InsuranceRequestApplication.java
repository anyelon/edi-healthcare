package com.example.edi.insurancerequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.edi.insurancerequest", "com.example.edi.common"})
public class InsuranceRequestApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsuranceRequestApplication.class, args);
    }
}
