package com.yuwen.magiccube.service;

import java.util.concurrent.CompletableFuture;

public interface DeepSeekService {

    /**
     * 根据心理问卷得分生成个性化建议
     * @param score 得分
     * @param interventionStatus 干预状态（PRE_INTERVENTION/POST_INTERVENTION）
     * @param userName 用户姓名
     * @return AI 生成的建议文本
     */
    String generateSuggestion(Integer score, String interventionStatus, String userName);

    /**
     * 异步生成心理建议
     * @param score 得分
     * @param interventionStatus 干预状态
     * @param userName 用户姓名
     * @return CompletableFuture 包含 AI 生成的建议文本
     */
    CompletableFuture<String> generateSuggestionAsync(Integer score, String interventionStatus, String userName);

    /**
     * 分析心理状态并生成详细报告
     * @param score 得分
     * @param interventionStatus 干预状态
     * @param userName 用户姓名
     * @param gender 性别
     * @param age 年龄
     * @return 详细的分析报告
     */
    String analyzePsychologicalState(Integer score, String interventionStatus,
                                     String userName, String gender, Integer age);
}
