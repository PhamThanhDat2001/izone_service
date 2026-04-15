package com.izone.izone_service.migration3.application.service.quiz;

import com.izone.izone_service.migration3.application.dto.CreateQuizCommand;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QuizMapper  {

    public CreateQuizCommand toCreateQuizCommand(V1ExerciseAggregate v1Dto,
                                                 List<CreateQuizCommand.CreateQuizQuestionCommand> quizQuestions) {

        if (v1Dto == null || v1Dto.getExercise() == null) return null;

        return CreateQuizCommand.builder()
                .name(v1Dto.getExercise().getName())
                .description(
                        (v1Dto.getExercise().getExamTitle() != null && !v1Dto.getExercise().getExamTitle().isBlank())
                                ? v1Dto.getExercise().getExamTitle()
                                : "Default Description for " + v1Dto.getExercise().getName()
                )
                .userId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .durationInMinutes(
                        "listening".equalsIgnoreCase(v1Dto.getExercise().getIeltType()) ? 40 : 60
                )
                .maxGrade(
                        (v1Dto.getExercise().getMaxScore() != null && v1Dto.getExercise().getMaxScore() > 0)
                                ? v1Dto.getExercise().getMaxScore().intValue()
                                : 10
                )
                .quizPages(mapToQuizPages(v1Dto.getParts(), quizQuestions))
                .tagIds(Collections.emptyList())
                .configuration(buildConfig(v1Dto.getExercise().getIeltType()))
                .conversionSchemeId(null)
                .thumbnail(null)
                .build();
    }

    private List<CreateQuizCommand.CreateQuizPageCommand> mapToQuizPages(
            List<V1ExerciseAggregate.V1PartDto> parts,
            List<CreateQuizCommand.CreateQuizQuestionCommand> quizQuestions) {

        if (parts == null || parts.isEmpty()) {
            return List.of(CreateQuizCommand.CreateQuizPageCommand.builder()
                    .name("General Section")
                    .description("Auto-generated section")
                    .sort(0)
                    .quizQuestions(quizQuestions)
                    .build());
        }

        return parts.stream().map(part ->
                CreateQuizCommand.CreateQuizPageCommand.builder()
                        .name(part.getTitle() != null ? part.getTitle() : "Untitled Section")
                        .description(part.getSubTitle() != null ? part.getSubTitle() : "No description provided")
                        .sort(part.getSort() != null ? part.getSort() : 0)
                        .quizQuestions(quizQuestions) // 👉 reuse chung
                        .build()
        ).collect(Collectors.toList());
    }

    private Object buildConfig(String ieltType) {
        if (ieltType == null) return null;

        String clazz;
        switch (ieltType.toLowerCase()) {
            case "listening": clazz = "IELTS_LISTENING"; break;
            case "reading": clazz = "IELTS_READING"; break;
            case "writing": clazz = "IELTS_WRITING"; break;
            case "speaking": clazz = "IELTS_SPEAKING"; break;
            default: clazz = "IELTS";
        }

        return Map.of("clazz", clazz);
    }
}