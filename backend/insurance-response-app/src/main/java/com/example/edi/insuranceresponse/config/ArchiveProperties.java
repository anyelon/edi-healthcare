package com.example.edi.insuranceresponse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.archive")
public record ArchiveProperties(String path) {}
