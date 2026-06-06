package org.example.ca_agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MetasoSearchProperties.class)
public class SearchToolConfig {

    @Bean
    public RestTemplate metasoRestTemplate(RestTemplateBuilder builder, MetasoSearchProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeout()))
                .build();
    }
}
