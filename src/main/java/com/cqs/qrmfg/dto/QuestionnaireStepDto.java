package com.cqs.qrmfg.dto;

import java.util.List;

public class QuestionnaireStepDto {
    private Integer stepNumber;
    private String stepTitle;
    private String category;
    private List<QuestionnaireTemplateDto> questions;
    private Integer totalQuestions;
    private Boolean isCompleted;

    public QuestionnaireStepDto() {}

    public QuestionnaireStepDto(Integer stepNumber, String stepTitle, String category) {
        this.stepNumber = stepNumber;
        this.stepTitle = stepTitle;
        this.category = category;
        this.isCompleted = false;
    }

    // Getters and setters
    public Integer getStepNumber() { return stepNumber; }
    public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

    public String getStepTitle() { return stepTitle; }
    public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<QuestionnaireTemplateDto> getQuestions() { return questions; }
    public void setQuestions(List<QuestionnaireTemplateDto> questions) { 
        this.questions = questions;
        this.totalQuestions = questions != null ? questions.size() : 0;
    }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
}