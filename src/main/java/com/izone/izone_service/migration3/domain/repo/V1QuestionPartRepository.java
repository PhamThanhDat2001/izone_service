package com.izone.izone_service.migration3.domain.repo;

import com.izone.izone_service.migration3.domain.modal.V1QuestionPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository doc du lieu tu bang api_questionpart trong V1 MySQL.
 * Question part tuong duong voi "section" hay "page" cua bai thi.
 */
public interface V1QuestionPartRepository extends JpaRepository<V1QuestionPart, Long> {

    /**
     * Doc danh sach question part theo danh sach ID.
     *
     * @param partIds danh sach ID cua cac question part can lay
     * @return danh sach question part sap xep theo id
     */
    @Query(
            value =
                    """
                    SELECT id, title, sub_title
                    FROM api_questionpart
                    WHERE id IN (:partIds)
                    ORDER BY id ASC
                    """,
            nativeQuery = true)
    List<V1QuestionPart> findByIdIn(@Param("partIds") List<Long> partIds);
}
