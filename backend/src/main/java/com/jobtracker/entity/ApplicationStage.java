package com.jobtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_stages")
public class ApplicationStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(nullable = false)
    private boolean isFinal;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime createdAt;

    protected ApplicationStage() {}

    public ApplicationStage(
            User user, String name, Integer position, String color, boolean isFinal) {
        this.user = user;
        this.name = name;
        this.position = position;
        this.color = color;
        this.isFinal = isFinal;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, Integer position, String color, boolean isFinal) {
        this.name = name;
        this.position = position;
        this.color = color;
        this.isFinal = isFinal;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public Integer getPosition() {
        return position;
    }

    public String getColor() {
        return color;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isActive() {
        return isActive;
    }
}
