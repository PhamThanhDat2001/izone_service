package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import com.izone.izone_service.migration3.application.dto.V1ExerciseDto;
import com.izone.izone_service.migration3.domain.modal.*;
import com.izone.izone_service.migration3.domain.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataReader {

    private final V1ExerciseRepository exerciseRepo;
    private final V1PartExerciseConnectionRepository partConnectionRepo;
    private final V1QuestionPartRepository questionPartRepo;
    private final V1QuestionRepository questionRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<V1ExerciseAggregate> readFullAggregates(int offset, int limit) {
        int page = offset / limit;

        // 1. Lấy exercise
        List<V1Exercise> exercises =
                exerciseRepo.findByForIeltTrue(PageRequest.of(page, limit)).getContent();

        if (exercises.isEmpty()) {
            log.warn("No exercises found");
            return Collections.emptyList();
        }

        List<Long> exerciseIds = exercises.stream()
                .map(V1Exercise::getId)
                .collect(Collectors.toList());

        // 2. Lấy connection
        List<V1PartExerciseConnection> connections =
                partConnectionRepo.findAllByIdExercisesIdIn(exerciseIds);

        // OPTIMIZE: group theo exerciseId (tránh O(n²))
        Map<Long, List<V1PartExerciseConnection>> connectionMap =
                connections.stream()
                        .collect(Collectors.groupingBy(c -> c.getId().getExercisesId()));

        // 3. Lấy partIds
        List<Long> partIds = connections.stream()
                .map(c -> c.getId().getQuestionPartId())
                .distinct()
                .collect(Collectors.toList());

        // 4. Lấy parts & questions
        List<V1QuestionPart> parts = questionPartRepo.findAllById(partIds);
        List<V1Question> questions = questionRepo.findAllByPartIdIn(partIds);

        Map<Long, V1QuestionPart> partMap = parts.stream()
                .collect(Collectors.toMap(V1QuestionPart::getId, p -> p));

        Map<Long, List<V1Question>> questionsByPartMap = questions.stream()
                .collect(Collectors.groupingBy(V1Question::getPartId));

        // 5. Build aggregate
        List<V1ExerciseAggregate> result = exercises.stream().map(ex -> {

            List<V1PartExerciseConnection> exConnections =
                    connectionMap.getOrDefault(ex.getId(), Collections.emptyList());

            List<V1ExerciseAggregate.V1PartDto> partDtos = exConnections.stream()
                    .map(conn -> {
                        Long pId = conn.getId().getQuestionPartId();
                        V1QuestionPart p = partMap.get(pId);
                        List<V1Question> qList =
                                questionsByPartMap.getOrDefault(pId, Collections.emptyList());

                        return V1ExerciseAggregate.V1PartDto.builder()
                                .id(pId)
                                .title(p != null ? p.getTitle() : "Untitled Part")
                                .subTitle(p != null ? p.getSubTitle() : "")
                                .sort(conn.getPartNumber() != null ? conn.getPartNumber() : 0)
                                .questions(qList.stream()
                                        .map(q -> V1ExerciseAggregate.V1QuestionDto.builder()
                                                .id(q.getId())
                                                .questionText(q.getQuestionText())
                                                .description(q.getDescription())
                                                .score(q.getScore())
                                                .questionType(q.getQuestionType())
                                                .groupName(q.getGroupName())
                                                .ieltType(q.getIeltType())
                                                .time(q.getTime())
                                                .isDeleted(q.getIsQDeleted())
                                                .createdAt(q.getCreatedAt())
                                                .updatedAt(q.getUpdatedAt())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build();
                    })
                    .collect(Collectors.toList());

            return V1ExerciseAggregate.builder()
                    .exercise(V1ExerciseDto.fromEntity(ex))
                    .parts(partDtos)
                    .build();

        }).collect(Collectors.toList());

        // ✅ LOG DATA CUỐI (QUAN TRỌNG)
        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            log.info("\n========== FINAL DATA ==========\n{}\n================================", json);

        } catch (Exception e) {
            log.error("Error when logging result", e);
        }

        return result;
    }
}