package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Entity read-only anh xa bang api_exercises trong V1 MySQL.
 * Khong co @GeneratedValue vi entity nay chi dung de doc du lieu.
 *
 * <p>Mapping sang V2:
 * <ul>
 *   <li>name          -> qe_quiz.name
 *   <li>exam_title    -> qe_quiz.description
 *   <li>deleted       -> qe_quiz.status (0=active->ACTIVE, 1=deleted->DRAFT)
 *   <li>ielt_time     -> qe_quiz.duration_in_minutes
 *   <li>max_score     -> qe_quiz.max_grade (double->int)
 *   <li>ielt_type     -> qe_quiz.configuration (JSON)
 * </ul>
 */
@Getter
@Entity
@Table(name = "api_exercises")
public class V1Exercise {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "exam_title")
    private String examTitle;

    /** 0 = active, 1 = deleted trong V1. Nguoc chieu voi V2 QuizStatus. */
    @Column(name = "deleted")
    private Integer deleted;

    @Column(name = "ielt_time")
    private Integer ieltTime;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "ielt_type")
    private String ieltType;
}
