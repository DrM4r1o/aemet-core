package com.technical.aemet.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.technical.aemet.domain.model.Forecast;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
public class ApplicationConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    Clock applicationClock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }

    @Bean
    Cache<String, Forecast> forecastCache(@Value("${forecast.cache-ttl:4h}") Duration ttl) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(10_000).build();
    }

    @Bean
    Cache<String, Forecast> forecastFallbackCache(
            @Value("${forecast.fallback-cache-ttl:24h}") Duration ttl,
            @Value("${forecast.fallback-cache-size:10_000}") long maximumSize) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maximumSize).build();
    }

    @Bean
    ThreadPoolTaskExecutor applicationTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("aemet-");
        executor.initialize();
        return executor;
    }
}
