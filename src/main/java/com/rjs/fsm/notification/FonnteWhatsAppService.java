package com.rjs.fsm.notification;

import com.rjs.fsm.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class FonnteWhatsAppService implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(FonnteWhatsAppService.class);
    private static final String FONNTE_API_URL = "https://api.fonnte.com/send";

    private final String apiKey;
    private final RestTemplate restTemplate;

    public FonnteWhatsAppService(AppProperties props) {
        this.apiKey = props.getFonnte().getApiKey();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean sendMessage(String phoneNumber, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Fonnte API key not configured. Message not sent to {}", phoneNumber);
            log.info("WA Message (dry-run) to {}: {}", phoneNumber, message);
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target", normalizePhone(phoneNumber));
            body.add("message", message);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(FONNTE_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WA message sent to {}", phoneNumber);
                return true;
            } else {
                log.error("Fonnte API error: status={}, body={}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send WA message to {}", phoneNumber, e);
            return false;
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+")) cleaned = cleaned.substring(1);
        // Convert local Indonesian format: 08xxx → 628xxx
        if (cleaned.startsWith("0")) cleaned = "62" + cleaned.substring(1);
        return cleaned;
    }
}
