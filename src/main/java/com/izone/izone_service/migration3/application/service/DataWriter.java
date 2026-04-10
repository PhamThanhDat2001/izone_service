package com.izone.izone_service.migration3.application.service;


import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class DataWriter {

    private final RestTemplate restTemplate;

    private static final String QUIZ_API_URL = "http://localhost:9090/v1/quizzes";

    // 🔥 API KEY của bạn
    private static final String API_KEY = "my-secure-api-key";

    public void writeQuiz(CreateQuizCommand command) {
        try {
            // 🔥 Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", API_KEY);

            // 🔥 Gói body + header
            HttpEntity<CreateQuizCommand> request =
                    new HttpEntity<>(command, headers);

            // 🔥 Gửi request
            restTemplate.postForObject(
                    QUIZ_API_URL,
                    request,
                    Void.class
            );

            System.out.println("✅ Tạo quiz thành công");

        } catch (Exception e) {
            System.out.println("❌ Gọi API tạo quiz thất bại: " + e.getMessage());
            throw e;
        }
    }
}