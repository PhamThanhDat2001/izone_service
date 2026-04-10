package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private final DataReader dataReader;
    private final DataMapping dataMapping;
    private final DataWriter dataWriter;

    public void migrate() {
        int offset = 0;
        int limit = 1;

//        while (true) {
            List<V1ExerciseAggregate> aggregates =
                    dataReader.readFullAggregates(offset, limit);

//            if (aggregates.isEmpty()) break;

            for (V1ExerciseAggregate agg : aggregates) {

                int maxRetries = 3;
                int attempt = 0;
                boolean success = false;

                while (!success) {
                    try {
                        CreateQuizCommand command =
                                dataMapping.toCreateQuizCommand(agg);

                        ObjectMapper objectMapper = new ObjectMapper();
                        String prettyJson = objectMapper
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(command);

                        System.out.println("Create Quiz Command:\n" + prettyJson);

                        dataWriter.writeQuiz(command);

                        success = true;
                        log.info("✅ Migrate thành công Exercise ID: {}",
                                agg.getExercise().getId());

                    } catch (HttpClientErrorException.Unauthorized e) {
                        // ❌ 401 → KHÔNG retry vô nghĩa
                        log.error("❌ Unauthorized (401) - dừng migrate luôn!");
                        return;

                    } catch (Exception e) {
                        attempt++;

                        log.warn("⚠️ Lỗi khi migrate Exercise ID: {} | attempt {}/{}",
                                agg.getExercise().getId(), attempt, maxRetries);

                        if (attempt >= maxRetries) {
                            log.error("❌ Bỏ qua Exercise ID: {} sau {} lần retry",
                                    agg.getExercise().getId(), maxRetries);
                            break;
                        }

                        // ⏳ Exponential backoff
                        try {
                            long delay = 3000L * attempt; // 3s, 6s, 9s
                            log.info("⏳ Đợi {} ms trước khi retry...", delay);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
//            }

            offset += limit;
        }
    }
}