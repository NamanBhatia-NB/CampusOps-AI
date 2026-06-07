package com.campusops.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "mock", matchIfMissing = true)
public class MockWhatsAppAdapter implements WhatsAppService {

    @Override
    public WhatsAppResult sendMessage(String to, String content) {
        log.info("MOCK WhatsApp: Sending to {}: {}", to, content);
        simulateDelay();
        return new WhatsAppResult(true, "mock-" + UUID.randomUUID().toString(), null);
    }

    @Override
    public WhatsAppResult sendTemplate(String to, String templateName, Map<String, String> variables) {
        log.info("MOCK WhatsApp: Sending template {} to {} with vars {}", templateName, to, variables);
        simulateDelay();
        return new WhatsAppResult(true, "mock-tpl-" + UUID.randomUUID().toString(), null);
    }

    private void simulateDelay() {
        try {
            Thread.sleep(150); // Simulate network call
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
