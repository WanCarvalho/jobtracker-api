package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByUserIdAndDeletedAtIsNullOrderByAppliedAtDesc(Long userId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    long countByUserIdAndDeletedAtIsNullAndStageIsFinal(Long userId, boolean isFinal);
}
