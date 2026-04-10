package com.izone.izone_service.migration3.domain.repo;


import com.izone.izone_service.migration3.domain.modal.V1PartExerciseConnection;
import com.izone.izone_service.migration3.domain.modal.V1PartExerciseConnectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository doc du lieu tu bang api_partexercisesconnection trong V1 MySQL.
 * Bang trung gian lien ket exercise voi question part.
 */
public interface V1PartConnectionRepository
        extends JpaRepository<V1PartExerciseConnection, V1PartExerciseConnectionId> {

    /**
     * Doc tat ca ket noi giua exercise va question part theo danh sach exercise ID.
     *
     * @param exerciseIds danh sach exercise ID can tra cuu
     * @return danh sach connection sap xep theo exercises_id va part_number
     */
    @Query(
            value =
                    """
                    SELECT exercises_id, question_part_id, part_number
                    FROM api_partexercisesconnection
                    WHERE exercises_id IN (:exerciseIds)
                    ORDER BY exercises_id, part_number ASC
                    """,
            nativeQuery = true)
    List<V1PartExerciseConnection> findByExerciseIdIn(
            @Param("exerciseIds") List<Long> exerciseIds);
}
