package com.izone.izone_service.migration3.application.service.quiz;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String URL = "http://localhost:9090/v1/quizzes";
    private static final String API_KEY = "my-secure-api-key";

    public void writeQuiz(CreateQuizCommand command) {

        try {
            // LOG REQUEST JSON
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(command);

            log.info("\n========== CREATE QUIZ REQUEST ==========\n{}\n=========================================", json);

        } catch (Exception e) {
            log.warn("Cannot serialize quiz request", e);
        }

        HttpEntity<CreateQuizCommand> request =
                new HttpEntity<>(command, buildHeaders());

        restTemplate.postForObject(URL, request, Void.class);

        log.info("Quiz created");
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }
}