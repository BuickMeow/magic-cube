package com.yuwen.magiccube.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "psychological_data")
public class PsychologicalData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    private Integer anxietyLevel;
    private Integer motivationLevel;
    private Integer focusLevel;

    @Column(name = "intervention_status")
    private String interventionStatus;

    @Column(name = "cd_risc_score")
    private Integer cdRiscScore;
    
    @Column(name = "student_name")
    private String studentName;
    
    @Column(name = "student_gender")
    private String studentGender;
    
    @Column(name = "student_age")
    private Integer studentAge;
        
    @Column(name = "student_grade")
    private String studentGrade;
        
    @Column(name = "created_at")
    private LocalDateTime createdAt;
        
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 设置默认干预状态
        if (this.interventionStatus == null) {
            this.interventionStatus = "PRE_INTERVENTION";
        }
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public void setTestDate(LocalDate testDate) {
        this.testDate = testDate;
    }

    public Integer getAnxietyLevel() {
        return anxietyLevel;
    }

    public void setAnxietyLevel(Integer anxietyLevel) {
        this.anxietyLevel = anxietyLevel;
    }

    public Integer getMotivationLevel() {
        return motivationLevel;
    }

    public void setMotivationLevel(Integer motivationLevel) {
        this.motivationLevel = motivationLevel;
    }

    public Integer getFocusLevel() {
        return focusLevel;
    }

    public void setFocusLevel(Integer focusLevel) {
        this.focusLevel = focusLevel;
    }

    public String getInterventionStatus() {
        return interventionStatus;
    }

    public void setInterventionStatus(String interventionStatus) {
        this.interventionStatus = interventionStatus;
    }

    public Integer getCdRiscScore() {
        return cdRiscScore;
    }

    public void setCdRiscScore(Integer cdRiscScore) {
        this.cdRiscScore = cdRiscScore;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentGender() {
        return studentGender;
    }

    public void setStudentGender(String studentGender) {
        this.studentGender = studentGender;
    }

    public Integer getStudentAge() {
        return studentAge;
    }

    public void setStudentAge(Integer studentAge) {
        this.studentAge = studentAge;
    }

    public String getStudentGrade() {
        return studentGrade;
    }

    public void setStudentGrade(String studentGrade) {
        this.studentGrade = studentGrade;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}