package com.jobtracker.repository;

import com.jobtracker.entity.ApplicationNote;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<ApplicationNote, Long> {
    List<ApplicationNote> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
