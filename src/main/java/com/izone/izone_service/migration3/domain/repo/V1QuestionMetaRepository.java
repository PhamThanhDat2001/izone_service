package com.izone.izone_service.migration3.domain.repo;

import com.izone.izone_service.migration3.domain.modal.V1QuestionMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository doc du lieu tu bang api_questionmeta trong V1 MySQL.
 * Moi question co the co nhieu meta records (cac input khac nhau).
 */
public interface V1QuestionMetaRepository extends JpaRepository<V1QuestionMeta, Long> {

    /**
     * Doc tat ca metadata cua cac question trong danh sach.
     * Batch load de tranh N+1 query problem.
     *
     * @param questionIds danh sach question ID can lay metadata
     * @return danh sach meta sap xep theo questions_id va id
     */
    @Query(
            value =
                    """
                    SELECT id, questions_id, meta_type, answer_score, manual_score, error
                    FROM api_questionmeta
                    WHERE questions_id IN (:questionIds)
                    ORDER BY questions_id, id ASC
                    """,
            nativeQuery = true)
    List<V1QuestionMeta> findByQuestionsIdIn(@Param("questionIds") List<Long> questionIds);
}
