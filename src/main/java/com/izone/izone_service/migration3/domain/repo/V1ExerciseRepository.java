package com.izone.izone_service.migration3.domain.repo;


import com.izone.izone_service.migration3.domain.modal.V1Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface V1ExerciseRepository extends JpaRepository<V1Exercise, Long> {
    Page<V1Exercise> findByForIeltTrue(Pageable pageable);

}
