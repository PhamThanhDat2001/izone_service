package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "api_questionpart")
public class V1QuestionPart {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "sub_title", columnDefinition = "TEXT")
    private String subTitle;

    @Column(name = "mark_type")
    private Integer markType;

    @Column(name = "file")
    private String file;

    @Column(name = "id_part")
    private String idPart;

    @Column(name = "exercise_type")
    private String exerciseType;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "is_q_deleted")
    private Boolean isQDeleted;
}