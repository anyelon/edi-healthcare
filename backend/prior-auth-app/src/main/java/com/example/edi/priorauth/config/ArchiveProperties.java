package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edi.archive")
public record ArchiveProperties(String path) {}
