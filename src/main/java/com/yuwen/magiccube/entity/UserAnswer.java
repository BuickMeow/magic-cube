package com.yuwen.magiccube.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_answers")
public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "questionnaire_id", nullable = false)
    private Integer questionnaireId;
    
    @Column(name = "question_id", nullable = false)
    private Integer questionId;
    
    @Column(name = "option_id")
    private Integer optionId; // 选择题的答案
    
    @Column(name = "scale_score")
    private Integer scaleScore; // 量表题的分数
    
    @Column(name = "answer_text", length = 1000)
    private String answerText; // 文本答案
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "intervention_status")
    private String interventionStatus;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
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

    public Integer getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(Integer questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getOptionId() {
        return optionId;
    }

    public void setOptionId(Integer optionId) {
        this.optionId = optionId;
    }

    public Integer getScaleScore() {
        return scaleScore;
    }

    public void setScaleScore(Integer scaleScore) {
        this.scaleScore = scaleScore;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getInterventionStatus() {
        return interventionStatus;
    }

    public void setInterventionStatus(String interventionStatus) {
        this.interventionStatus = interventionStatus;
    }
}
