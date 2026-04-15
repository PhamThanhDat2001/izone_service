package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "api_questions")
public class V1Question {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "score")
    private Double score;

    @Column(name = "file")
    private String file;

    @Column(name = "part_id")
    private Long partId;

    @Column(name = "question_type")
    private String questionType;

    @Column(name = "sub_title", columnDefinition = "TEXT")
    private String subTitle;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "ielt_type")
    private String ieltType;

    @Column(name = "is_q_deleted")
    private Boolean isQDeleted;

    @Column(name = "time")
    private Integer time;
}