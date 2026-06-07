package com.campusops.integration;

import com.campusops.config.AppConfig;
import com.campusops.entity.Lead;
import com.campusops.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.whatsapp.provider", havingValue = "meta")
@RequiredArgsConstructor
@Slf4j
public class MetaWhatsAppAdapter implements WhatsAppService {

    private final AppConfig appConfig;
    private final RestTemplate restTemplate;

    private static final String META_GRAPH_URL = "https://graph.facebook.com/v19.0/";

    @Override
    public WhatsAppResult sendTemplate(String to, String templateName, Map<String, String> variables) {
        log.info("Sending Meta WhatsApp template {} to phone {}", templateName, to);
        return sendMessagePayload(to, buildTemplatePayload(to, templateName, variables));
    }

    @Override
    public WhatsAppResult sendMessage(String to, String content) {
        log.info("Sending Meta WhatsApp text to phone {}: {}", to, content);
        return sendMessagePayload(to, buildTextPayload(to, content));
    }

    private WhatsAppResult sendMessagePayload(String toPhone, Map<String, Object> payload) {
        String token = appConfig.getWhatsapp().getMeta().getToken();
        String phoneId = appConfig.getWhatsapp().getMeta().getPhoneNumberId();

        if (token == null || token.isBlank() || phoneId == null || phoneId.isBlank()) {
            log.error("Meta WhatsApp credentials not configured. Please set META_WHATSAPP_TOKEN and META_PHONE_NUMBER_ID.");
            return new WhatsAppResult(false, null, "Meta WhatsApp credentials not configured");
        }

        String url = META_GRAPH_URL + phoneId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return new WhatsAppResult(true, "meta-id-" + System.currentTimeMillis(), null);
            } else {
                return new WhatsAppResult(false, null, "API Error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send Meta WhatsApp message", e);
            return new WhatsAppResult(false, null, e.getMessage());
        }
    }

    private Map<String, Object> buildTextPayload(String to, String text) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");
        
        Map<String, String> textObj = new HashMap<>();
        textObj.put("body", text);
        payload.put("text", textObj);
        
        return payload;
    }

    private Map<String, Object> buildTemplatePayload(String to, String templateName, Map<String, String> variables) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "template");
        
        Map<String, Object> templateObj = new HashMap<>();
        templateObj.put("name", templateName);
        
        Map<String, String> langObj = new HashMap<>();
        langObj.put("code", "en_US");
        templateObj.put("language", langObj);
        
        // Add variables mapping here based on Meta's component array structure
        // Simplified for this adapter
        
        payload.put("template", templateObj);
        
        return payload;
    }
}
