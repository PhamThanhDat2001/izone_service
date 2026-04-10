package com.izone.izone_service.migration3.domain.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Entity read-only anh xa bang api_questionpart trong V1 MySQL.
 *
 * <p>Mapping sang V2:
 * <ul>
 *   <li>title     -> qe_quiz_page.name
 *   <li>sub_title -> qe_quiz_page.description
 * </ul>
 */
@Getter
@Entity
@Table(name = "api_questionpart")
public class V1QuestionPart {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "sub_title")
    private String subTitle;
}
