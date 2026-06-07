package com.campusops.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Application-level bean definitions and typed configuration properties
 * bound from the {@code app.*} namespace.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {

    // ─── Typed properties bound from application.yml ─────────────────

    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();
    private Demo demo = new Demo();
    private Whatsapp whatsapp = new Whatsapp();

    @Getter @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private long refreshExpirationMs;
    }

    @Getter @Setter
    public static class Ai {
        private String provider;
    }

    @Getter @Setter
    public static class Demo {
        private boolean enabled;
        private boolean seedData;
        private String adminEmail;
        private String adminPassword;
    }

    @Getter @Setter
    public static class Whatsapp {
        private String provider;
        private Meta meta = new Meta();
        
        @Getter @Setter
        public static class Meta {
            private String token;
            private String phoneNumberId;
        }
    }

    // ─── Bean definitions ────────────────────────────────────────────

    /**
     * Pre-configured {@link RestTemplate} with sensible timeouts.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Customised Jackson {@link ObjectMapper} registered as the primary bean.
     * <ul>
     *   <li>Java 8 date/time support via {@link JavaTimeModule}</li>
     *   <li>Timestamps written as ISO-8601 strings</li>
     *   <li>Unknown JSON properties silently ignored</li>
     * </ul>
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
