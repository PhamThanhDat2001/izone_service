package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * Composite primary key cho bang api_partexercisesconnection.
 */
@Getter
@Embeddable
@EqualsAndHashCode
public class V1PartExerciseConnectionId implements Serializable {

    @Column(name = "exercises_id")
    private Long exercisesId;

    @Column(name = "question_part_id")
    private Long questionPartId;
}
