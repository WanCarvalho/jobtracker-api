package com.jobtracker.entity;

import com.jobtracker.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, unique=true, length=180) private String email;
    @Column(nullable=false) private String password;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private UserRole role;
    @Column(nullable=false) private LocalDateTime createdAt;
    protected User() {}
    public User(String name, String email, String password, UserRole role) {
        this.name=name; this.email=email; this.password=password; this.role=role; this.createdAt=LocalDateTime.now();
    }
    public Long getId(){return id;} public String getName(){return name;} public String getEmail(){return email;}
    public String getPassword(){return password;} public UserRole getRole(){return role;} public LocalDateTime getCreatedAt(){return createdAt;}
}
