package com.izone.izone_service.migration3.application.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankEntryCreateRequest {

    private String name;
    private Long parentId;
    private String type; // ⚠️ dùng String thay vì enum
    private String description;
    private Integer defaultWeight;
    private String generalFeedback;
    private UUID ownerId;
    private List<CreateQuestionInputRequest> questionInputs;
    private List<Long> tagIds;
    private Object metadata;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuestionInputRequest {
        private Integer weight;
        private String type;
        private Object scoringMetadata;
        private Object questionMetadata;
    }
}