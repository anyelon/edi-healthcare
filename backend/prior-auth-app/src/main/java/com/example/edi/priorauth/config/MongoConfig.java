package com.example.edi.priorauth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.edi.common.repository")
@EnableConfigurationProperties(ArchiveProperties.class)
public class MongoConfig {
}
