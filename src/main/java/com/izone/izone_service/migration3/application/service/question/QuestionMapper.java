package com.izone.izone_service.migration3.application.service.question;

import com.izone.izone_service.migration3.application.dto.QuestionBankEntryCreateRequest;
import com.izone.izone_service.migration3.application.dto.V1ExerciseAggregate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class QuestionMapper {

    public QuestionBankEntryCreateRequest toRequest(V1ExerciseAggregate.V1QuestionDto q) {
        if (q == null) return null;

        return QuestionBankEntryCreateRequest.builder()
                .name("Question " + q.getId())
                .type("MULTIPLE_CHOICE")
                .description("Auto migrated question " + q.getId())
                .defaultWeight(
                        (q.getScore() != null && q.getScore() >= 1)
                                ? q.getScore().intValue()
                                : 1
                )
                .ownerId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .questionInputs(Collections.emptyList())
                .tagIds(Collections.emptyList())
                .metadata(null)
                .build();
    }
}