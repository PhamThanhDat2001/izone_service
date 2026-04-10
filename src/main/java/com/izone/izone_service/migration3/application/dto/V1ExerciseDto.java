package com.izone.izone_service.migration3.application.dto;

import com.izone.izone_service.migration3.domain.modal.V1Exercise;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO đại diện cho dữ liệu từ V1 (api_exercises)
 * phục vụ cho quá trình migration sang V2 (qe_quiz).
 */
@Getter
@Builder
public class V1ExerciseDto {

    private Long id;                // Mapping: id (V1)
    private String name;            // Mapping: qe_quiz.name
    private String examTitle;       // Mapping: qe_quiz.description
    private Integer deleted;        // Mapping logic: 0 -> ACTIVE, 1 -> DRAFT (qe_quiz.status)
    private Integer ieltTime;       // Mapping: qe_quiz.duration_in_minutes
    private Double maxScore;        // Mapping: qe_quiz.max_grade
    private String ieltType;        // Mapping: qe_quiz.configuration (Dạng JSON chứa clazz,...)

    /**
     * Phương thức hỗ trợ chuyển đổi từ Entity sang DTO nhanh
     */
    public static V1ExerciseDto fromEntity(V1Exercise entity) {
        return V1ExerciseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .examTitle(entity.getExamTitle())
                .deleted(entity.getDeleted())
                .ieltTime(entity.getIeltTime())
                .maxScore(entity.getMaxScore())
                .ieltType(entity.getIeltType())
                .build();
    }
}
