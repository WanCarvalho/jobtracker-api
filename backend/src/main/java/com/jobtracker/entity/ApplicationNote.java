package com.jobtracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="application_notes")
public class ApplicationNote {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="application_id",nullable=false) private JobApplication application;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(nullable=false,columnDefinition="TEXT") private String content;
 private LocalDateTime createdAt;
 protected ApplicationNote(){}
 public ApplicationNote(JobApplication a,User u,String c){application=a;user=u;content=c;createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getContent(){return content;} public LocalDateTime getCreatedAt(){return createdAt;} public User getUser(){return user;}
}
