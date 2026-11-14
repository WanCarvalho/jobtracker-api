package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationStage;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<ApplicationStage, Long> {
    List<ApplicationStage> findByUserIdAndIsActiveTrueOrderByPosition(Long userId);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
