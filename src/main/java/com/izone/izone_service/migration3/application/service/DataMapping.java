package com.izone.izone_service.migration3.application.service;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataMapping {

    public CreateQuizCommand toCreateQuizCommand(V1ExerciseAggregate v1Dto) {
        if (v1Dto == null || v1Dto.getExercise() == null) return null;

        return CreateQuizCommand.builder()
                .name(v1Dto.getExercise().getName())
                // Description không được null theo @NotNull
                .description(
                        (v1Dto.getExercise().getExamTitle() != null && !v1Dto.getExercise().getExamTitle().isBlank())
                                ? v1Dto.getExercise().getExamTitle()
                                : "Default Description for " + v1Dto.getExercise().getName()
                )
                .durationInMinutes(
                        (v1Dto.getExercise().getIeltTime() != null && v1Dto.getExercise().getIeltTime() > 0)
                                ? v1Dto.getExercise().getIeltTime()
                                : 1 // @Min(1)
                )
                .maxGrade(
                        (v1Dto.getExercise().getMaxScore() != null && v1Dto.getExercise().getMaxScore() > 0)
                                ? v1Dto.getExercise().getMaxScore().intValue()
                                : 10 // @Min(1)
                )
                // Map list Parts từ V1 sang QuizPages (Sections)
                .quizPages(mapToQuizPages(v1Dto.getParts()))

                .tagIds(Collections.emptyList())
                .configuration(null) // Sẽ bổ sung nếu có logic map ielt_type
                .conversionSchemeId(null)
                .thumbnail(null)
                .build();
    }

    private List<CreateQuizCommand.CreateQuizPageCommand> mapToQuizPages(List<V1ExerciseAggregate.V1PartDto> parts) {
        if (parts == null || parts.isEmpty()) {
            // Theo @NotEmpty, một quiz phải có ít nhất 1 section.
            // Nếu data V1 không có part, ta tạo 1 section mặc định để tránh lỗi API.
            return List.of(CreateQuizCommand.CreateQuizPageCommand.builder()
                    .name("General Section")
                    .description("Auto-generated section")
                    .sort(0)
                    .quizQuestions(Collections.emptyList()) // Lưu ý: @NotEmpty ở quizQuestions nữa
                    .build());
        }

        return parts.stream().map(part ->
                CreateQuizCommand.CreateQuizPageCommand.builder()
                        .name(part.getTitle() != null ? part.getTitle() : "Untitled Section")
                        .description(part.getSubTitle() != null ? part.getSubTitle() : "No description provided")
                        .sort(part.getSort() != null ? part.getSort() : 0)
                        .quizQuestions(mapToQuizQuestions(part.getQuestions()))
                        .build()
        ).collect(Collectors.toList());
    }

    private List<CreateQuizCommand.CreateQuizQuestionCommand> mapToQuizQuestions(List<V1ExerciseAggregate.V1QuestionDto> questions) {
        if (questions == null || questions.isEmpty()) return Collections.emptyList();

        return questions.stream().map(q ->
                CreateQuizCommand.CreateQuizQuestionCommand.builder()
                        .questionId(q.getId())
                        .weight(q.getScore() != null ? q.getScore().intValue() : 1)
                        .sort(0) // Bạn có thể thêm logic lấy số thứ tự câu hỏi ở đây
                        .build()
        ).collect(Collectors.toList());
    }
}