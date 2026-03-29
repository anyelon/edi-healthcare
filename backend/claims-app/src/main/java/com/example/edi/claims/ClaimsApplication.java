package com.example.edi.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.edi.claims", "com.example.edi.common"})
public class ClaimsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimsApplication.class, args);
    }
}
