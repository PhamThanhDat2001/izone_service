package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import com.izone.izone_service.migration3.application.dto.V1ExerciseDto;
import com.izone.izone_service.migration3.domain.modal.*;
import com.izone.izone_service.migration3.domain.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataReader {
    private final V1ExerciseRepository exerciseRepo;
    private final V1PartExerciseConnectionRepository partConnectionRepo; // Đã đổi tên Repo
    private final V1QuestionPartRepository questionPartRepo;
    private final V1QuestionRepository questionRepo;

    public List<V1ExerciseAggregate> readFullAggregates(int offset, int limit) {
        int page = offset / limit;
        List<V1Exercise> exercises = exerciseRepo.findAll(PageRequest.of(page, limit)).getContent();
        if (exercises.isEmpty()) return Collections.emptyList();

        List<Long> exerciseIds = exercises.stream().map(V1Exercise::getId).collect(Collectors.toList());

        // 2. Lấy Connections sử dụng Entity V1PartExerciseConnection mới của bạn
        // Truy vấn dựa trên trường exercisesId nằm trong @EmbeddedId
        List<V1PartExerciseConnection> connections = partConnectionRepo.findAllByIdExercisesIdIn(exerciseIds);

        List<Long> partIds = connections.stream()
                .map(c -> c.getId().getQuestionPartId()) // Lấy từ EmbeddedId
                .distinct()
                .collect(Collectors.toList());

        // 3. Lấy Parts và Questions
        List<V1QuestionPart> parts = questionPartRepo.findAllById(partIds);
        List<V1Question> questions = questionRepo.findAllByPartIdIn(partIds);

        Map<Long, V1QuestionPart> partMap = parts.stream()
                .collect(Collectors.toMap(V1QuestionPart::getId, p -> p));
        Map<Long, List<V1Question>> questionsByPartMap = questions.stream()
                .collect(Collectors.groupingBy(V1Question::getPartId));

        // 4. Tổ chức lại dữ liệu vào Aggregate
        return exercises.stream().map(ex -> {
            List<V1ExerciseAggregate.V1PartDto> partDtos = connections.stream()
                    .filter(c -> c.getId().getExercisesId().equals(ex.getId())) // So sánh ID từ EmbeddedId
                    .map(conn -> {
                        Long pId = conn.getId().getQuestionPartId();
                        V1QuestionPart p = partMap.get(pId);
                        List<V1Question> qList = questionsByPartMap.getOrDefault(pId, Collections.emptyList());

                        return V1ExerciseAggregate.V1PartDto.builder()
                                .id(pId)
                                .title(p != null ? p.getTitle() : "Untitled Part")
                                .subTitle(p != null ? p.getSubTitle() : "")
                                .sort(conn.getPartNumber() != null ? conn.getPartNumber() : 0)
                                .questions(qList.stream().map(q -> V1ExerciseAggregate.V1QuestionDto.builder()
                                        .id(q.getId())
                                        .score(q.getScore())
                                        .build()).collect(Collectors.toList()))
                                .build();
                    }).collect(Collectors.toList());

            return V1ExerciseAggregate.builder()
                    .exercise(V1ExerciseDto.fromEntity(ex))
                    .parts(partDtos)
                    .build();
        }).collect(Collectors.toList());
    }
}