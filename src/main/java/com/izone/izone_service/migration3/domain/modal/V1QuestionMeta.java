package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Entity read-only anh xa bang api_questionmeta trong V1 MySQL.
 *
 * <p>Mapping sang V2:
 * <ul>
 *   <li>meta_type     -> qe_question_input.type (string->int enum)
 *   <li>answer_score  -> qe_question_input.weight, qe_question_input.scoring_metadata (JSON)
 *   <li>manual_score  -> qe_question_input.scoring_metadata (JSON)
 *   <li>error         -> qe_question.general_feedback (phan hoi chung)
 * </ul>
 */
@Getter
@Entity
@Table(name = "api_questionmeta")
public class V1QuestionMeta {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "meta_type")
    private String metaType;

    @Column(name = "answer_score")
    private Double answerScore;

    @Column(name = "manual_score")
    private Double manualScore;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
}
