package com.izone.izone_service.migration3.application.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V1ExerciseAggregate {

    private V1ExerciseDto exercise;
    private List<V1PartDto> parts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V1PartDto {
        private Long id;
        private String title;
        private String subTitle;
        private Integer sort;

        private Integer markType;
        private String exerciseType;

        private List<V1QuestionDto> questions;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class V1QuestionDto {
        private Long id;

        private String questionText;
        private String description;

        private Double score;
        private String questionType;

        private String groupName;
        private String ieltType;

        private Integer time;

        private Boolean isDeleted;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}