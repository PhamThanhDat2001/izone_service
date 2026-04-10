package com.izone.izone_service.migration3.domain.repo;

import com.izone.izone_service.migration3.domain.modal.V1Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository doc du lieu tu bang api_questions trong V1 MySQL.
 */
public interface V1QuestionRepository extends JpaRepository<V1Question, Long> {
    List<V1Question> findAllByPartIdIn(List<Long> partIds);
    /**
     * Doc danh sach question thuoc cac question part cu the.
     * Dung de lay tat ca cau hoi cua mot bai thi.
     *
     * @param questionPartIds danh sach question_part_id (parent)
     * @return danh sach question sap xep theo question_part_id va id
     */
    @Query(
            value =
                    """
                    SELECT id, question_part_id, question_text, question_type,
                           description, score, is_q_deleted, created_at, updated_at
                    FROM api_questions
                    WHERE question_part_id IN (:questionPartIds)
                    ORDER BY question_part_id, id ASC
                    """,
            nativeQuery = true)
    List<V1Question> findByQuestionPartIdIn(
            @Param("questionPartIds") List<Long> questionPartIds);
}
