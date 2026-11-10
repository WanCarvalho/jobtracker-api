package com.jobtracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="job_applications")
public class JobApplication {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id",nullable=false) private Company company;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="stage_id",nullable=false) private ApplicationStage stage;
 @Column(nullable=false,length=180) private String jobTitle;
 private BigDecimal salary;
 @Column(length=1000) private String jobUrl;
 @Column(columnDefinition="TEXT") private String description;
 @Column(nullable=false) private LocalDate appliedAt;
 private LocalDateTime createdAt; private LocalDateTime updatedAt;
 protected JobApplication(){}
 public JobApplication(User user,Company company,ApplicationStage stage,String title,BigDecimal salary,String url,String description,LocalDate appliedAt){this.user=user;this.company=company;this.stage=stage;this.jobTitle=title;this.salary=salary;this.jobUrl=url;this.description=description;this.appliedAt=appliedAt;this.createdAt=LocalDateTime.now();this.updatedAt=this.createdAt;}
 public void update(Company company,ApplicationStage stage,String title,BigDecimal salary,String url,String description,LocalDate appliedAt){this.company=company;this.stage=stage;this.jobTitle=title;this.salary=salary;this.jobUrl=url;this.description=description;this.appliedAt=appliedAt;this.updatedAt=LocalDateTime.now();}
 public Long getId(){return id;} public User getUser(){return user;} public Company getCompany(){return company;} public ApplicationStage getStage(){return stage;} public String getJobTitle(){return jobTitle;} public BigDecimal getSalary(){return salary;} public String getJobUrl(){return jobUrl;} public String getDescription(){return description;} public LocalDate getAppliedAt(){return appliedAt;}
}
