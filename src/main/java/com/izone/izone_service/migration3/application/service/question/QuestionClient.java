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

        // POST /v1/question-bank-entries trả về QuestionBankEntryViewResult.
        // Tuy nhiên field "questions" KHÔNG được populate trong response của create()
        // (lms-service chỉ map entity trực tiếp, chưa fetch versions kèm theo).
        // → Cần gọi thêm GET /v1/question-bank-entries/{bankEntryId}/versions/1
        //   để lấy qe_question.id thực sự cần gán vào quiz.
        Number bankEntryId = (Number) response.get("id");

        log.info("Created question bank entry id={}, đang lấy question id...", bankEntryId);

        // Gọi GET /v1/question-bank-entries/{id}/versions/1
        // Trả về QuestionWithRelationsResult có field "id" = qe_question.id
        String versionUrl = URL + "/" + bankEntryId + "/versions/1";
        Map versionResponse = restTemplate.exchange(
                versionUrl,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(buildHeaders()),
                Map.class
        ).getBody();

        if (versionResponse == null) {
            throw new RuntimeException(
                    "Không lấy được question version cho bankEntryId=" + bankEntryId);
        }

        // "id" trong QuestionWithRelationsResult = qe_question.id (đây mới là questionId cần dùng)
        Number questionId = (Number) versionResponse.get("id");

        log.info("✅ Created question: bankEntryId={}, questionId={}", bankEntryId, questionId.longValue());

        return questionId.longValue();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }
}