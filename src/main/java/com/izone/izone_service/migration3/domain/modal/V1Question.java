package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Entity read-only anh xa bang api_questions trong V1 MySQL.
 *
 * <p>Luu y kieu du lieu:
 * <ul>
 *   <li>is_q_deleted la Integer (khong phai Boolean) de tranh MySQL tinyint(1) tu dong convert sang boolean
 *   <li>created_at/updated_at la datetime(6) trong MySQL, map sang LocalDateTime voi serverTimezone trong JDBC URL
 * </ul>
 *
 * <p>Mapping sang V2:
 * <ul>
 *   <li>question_text  -> qe_question.name, qe_question_bank_entry.latest_name
 *   <li>question_type  -> qe_question.type (string->int enum)
 *   <li>description    -> qe_question.description, qe_question_bank_entry.latest_description
 *   <li>score          -> qe_question.default_weight, qe_quiz_question.weight
 *   <li>is_q_deleted   -> qe_question_bank_entry.is_deleted
 *   <li>created_at     -> qe_question_bank_entry.creation_datetime
 *   <li>updated_at     -> qe_question_bank_entry.update_datetime
 * </ul>
 */
@Getter
@Entity
@Table(name = "api_questions")
public class V1Question {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "part_id")
    private Long partId;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "score")
    private Double score;

    /**
     * Dung Integer thay vi Boolean de tranh MySQL JDBC driver tu dong convert tinyint(1).
     * 0 = chua xoa, 1 = da xoa.
     */
    @Column(name = "is_q_deleted", columnDefinition = "TINYINT")
    private Integer isQDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
