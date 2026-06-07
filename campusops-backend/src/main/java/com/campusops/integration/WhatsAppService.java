package com.campusops.integration;

import java.util.Map;

public interface WhatsAppService {
    WhatsAppResult sendMessage(String to, String content);
    WhatsAppResult sendTemplate(String to, String templateName, Map<String, String> variables);

    record WhatsAppResult(boolean success, String messageId, String error) {}
}
