package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<JobApplication,Long> {
 List<JobApplication> findByUserIdOrderByAppliedAtDesc(Long userId);
 long countByUserId(Long userId);
 long countByUserIdAndStageIsFinal(Long userId,boolean isFinal);
}
