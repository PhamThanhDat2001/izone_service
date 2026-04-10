package com.izone.izone_service.migration3.domain.repo;

import com.izone.izone_service.migration3.domain.modal.V1PartExerciseConnection;
import com.izone.izone_service.migration3.domain.modal.V1PartExerciseConnectionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface V1PartExerciseConnectionRepository extends JpaRepository<V1PartExerciseConnection, V1PartExerciseConnectionId> {
    
    /**
     * Spring Data JPA sẽ tự động phân tích: 
     * id (EmbeddedId) -> exercisesId (Trường bên trong Id)
     */
    List<V1PartExerciseConnection> findAllByIdExercisesIdIn(List<Long> exerciseIds);
}