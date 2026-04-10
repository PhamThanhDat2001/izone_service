package com.izone.izone_service.migration3.domain.repo;


import com.izone.izone_service.migration3.domain.modal.V1Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository doc du lieu tu bang api_exercises trong V1 MySQL.
 * Su dung JPA Native Query de dam bao tuong thich voi MySQL syntax.
 * count() ke thua tu JpaRepository.
 */
public interface V1ExerciseRepository extends JpaRepository<V1Exercise, Long> {

}
