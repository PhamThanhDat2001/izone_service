package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Entity read-only anh xa bang api_partexercisesconnection trong V1 MySQL.
 * Bang nay la bang trung gian lien ket exercise voi question part.
 *
 * <p>Mapping sang V2:
 * <ul>
 *   <li>exercises_id      -> qe_quiz_question.quiz_id (qua IdMappingRegistry)
 *   <li>question_part_id  -> qe_quiz_page (dung de xac dinh trang)
 *   <li>part_number       -> qe_quiz_page.sort
 * </ul>
 */
@Getter
@Entity
@Table(name = "api_partexercisesconnection")
public class V1PartExerciseConnection {

    @EmbeddedId
    private V1PartExerciseConnectionId id;

    @Column(name = "part_number")
    private Integer partNumber;
}
