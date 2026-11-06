package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationHistory;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryRepository extends JpaRepository<ApplicationHistory,Long> { List<ApplicationHistory> findByApplicationIdOrderByChangedAtDesc(Long applicationId); }
