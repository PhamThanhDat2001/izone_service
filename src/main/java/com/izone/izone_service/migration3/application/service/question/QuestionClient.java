package com.izone.izone_service.migration3.application.service.question;

import com.izone.izone_service.migration3.application.dto.QuestionBankEntryCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String URL = "http://localhost:9090/v1/question-bank-entries";
    private static final String API_KEY = "my-secure-api-key";

    public Long create(QuestionBankEntryCreateRequest req) {
        try {
            // 🔥 LOG REQUEST JSON
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(req);

            log.info("\n========== CREATE QUESTION REQUEST ==========\n{}\n============================================", json);

        } catch (Exception e) {
            log.warn("⚠️ Cannot serialize question request", e);
        }

        HttpEntity<QuestionBankEntryCreateRequest> request =
                new HttpEntity<>(req, buildHeaders());

        Map response = restTemplate.postForObject(URL, request, Map.class);

        Integer id = (Integer) response.get("id");

        log.info("✅ Created question newId={}", id);

        return id.longValue();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }
}