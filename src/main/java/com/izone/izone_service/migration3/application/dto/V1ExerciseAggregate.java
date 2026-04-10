package com.izone.izone_service.migration3.application.dto;

import lombok.*;
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
    public static class V1PartDto {
        private Long id;
        private String title;
        private String subTitle;
        private Integer sort; // Lấy từ part_number
        private List<V1QuestionDto> questions;
    }

    @Getter
    @Builder
    public static class V1QuestionDto {
        private Long id;
        private Double score;
    }
}