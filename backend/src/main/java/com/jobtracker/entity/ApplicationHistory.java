package com.jobtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_history")
public class ApplicationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_stage_id")
    private ApplicationStage previousStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_stage_id", nullable = false)
    private ApplicationStage newStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime changedAt;

    protected ApplicationHistory() {}

    public ApplicationHistory(
            JobApplication app,
            ApplicationStage previous,
            ApplicationStage next,
            User user,
            String notes) {
        this.application = app;
        this.previousStage = previous;
        this.newStage = next;
        this.changedBy = user;
        this.notes = notes;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ApplicationStage getPreviousStage() {
        return previousStage;
    }

    public ApplicationStage getNewStage() {
        return newStage;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public User getChangedBy() {
        return changedBy;
    }
}
