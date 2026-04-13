package com.izone.izone_service.migration3.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizCommand {

    @Builder.Default
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 65535, message = "Description must be at most 65535 characters")
    @NotNull(message = "Description quiz is required")
    private String description;

    @Min(value = 1, message = "The quiz duration must be greater than 0")
    private Integer durationInMinutes;

    @NotEmpty(message = "The quiz must have at least one section")
    @Valid
    private List<CreateQuizPageCommand> quizPages;

    @Min(value = 1, message = "The quiz Max Grade must be greater than 0")
    private Integer maxGrade;

    private List<Long> tagIds;

    private Object configuration;

    private Long conversionSchemeId;

    private UUID thumbnail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuizPageCommand {
        @NotBlank(message = "Section name must not be empty")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        private String name;

        @Size(max = 65535, message = "Section description must not exceed 65535 characters")
        @NotNull(message = "Description quiz page is required")
        private String description;

        @Min(value = 0, message = "Order must be greater than or equal to 0")
        @NotNull(message = "Sort is required")
        private Integer sort;

        private Object  configMetadata;

        private Object  scoringMetadata;

        @NotEmpty(message = "Each section must contain at least one question")
        @Valid
        private List<CreateQuizQuestionCommand> quizQuestions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuizQuestionCommand {
        @NotNull(message = "Question ID must not be empty")
        private Long questionId;

        private Integer weight;

        @Min(value = 0, message = "Order must be greater than or equal to 0")
        @NotNull(message = "Sort is required")
        private Integer sort;
    }
}
