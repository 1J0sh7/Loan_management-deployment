package com.company.health;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SendGridHealthIndicator implements HealthIndicator {

    private final SendGrid sendGrid;
    private final String apiKey;

    public SendGridHealthIndicator(SendGrid sendGrid,
                                   @Value("${sendgrid.api-key}") String apiKey) {
        this.sendGrid = sendGrid;
        this.apiKey = apiKey;
    }

    @Override
    public Health health() {
        try {
            // 1. Check if the API key is actually present in config
            if (apiKey == null || apiKey.isBlank()) {
                return Health.down()
                        .withDetail("service", "SendGrid")
                        .withDetail("error", "API key is missing or empty in configuration")
                        .build();
            }

            // 2. Check if the SendGrid client was successfully instantiated
            // (If Spring created the bean, it usually means the client initialized okay).
            if (sendGrid == null) {
                return Health.down()
                        .withDetail("service", "SendGrid")
                        .withDetail("error", "SendGrid client failed to initialize")
                        .build();
            }

            // If we got here, the key exists and the client is alive
            return Health.up()
                    .withDetail("service", "SendGrid")
                    .withDetail("status", "API key is configured and client is ready")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "SendGrid")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}