package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.PsychologicalDataRepository;
import com.yuwen.magiccube.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
@Service
public class PsychologicalDataService {
    
    @Autowired
    private PsychologicalDataRepository repository;
    
    @Autowired
    private UserRepository userRepository;
    
    // 获取 repository（供其他服务类使用）
    public PsychologicalDataRepository getRepository() {
        return repository;
    }
    
    // 获取 userRepository（供其他服务类使用）
    public UserRepository getUserRepository() {
        return userRepository;
    }
    
    // 获取用户的最新心理数据（带 CD-RISC 分数，按 created_at 倒序）
    public PsychologicalData getLatestUserDataWithCDRISC(Integer userId) {
        List<PsychologicalData> dataList = repository.findByUserIdAndCdRiscScoreIsNotNullOrderByCreatedAtDesc(userId);
        if (dataList != null && !dataList.isEmpty()) {
            return dataList.get(0);
        }
        return null;
    }

    // 获取用户的所有心理数据，按日期正序排列
    public List<PsychologicalData> getUserData(Integer userId) {
        return repository.findByUserIdOrderByTestDateAsc(userId);
    }

    // 获取用户的最新心理数据
    public PsychologicalData getLatestUserData(Integer userId) {
        List<PsychologicalData> dataList = repository.findByUserIdOrderByTestDateDesc(userId);
        if (dataList != null && !dataList.isEmpty()) {
            return dataList.get(0);
        }
        return null;
    }

    // 添加/更新心理数据
    public void addData(Integer userId, Integer score, LocalDate testDate) {
        PsychologicalData data = new PsychologicalData();
        data.setUserId(userId);
        data.setScore(score);
        data.setTestDate(testDate);
        repository.save(data);
    }
    
    public void addCDRISCData(Integer userId, Integer cdRiscScore, LocalDate testDate, String interventionStatus) {
        PsychologicalData data = new PsychologicalData();
        data.setUserId(userId);
        data.setCdRiscScore(cdRiscScore);
        data.setTestDate(testDate);
        data.setInterventionStatus(interventionStatus);
        data.setScore(cdRiscScore);
        
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            data.setStudentName(user.getName());
            data.setStudentGender(user.getGender());
            data.setStudentAge(user.getAge());
            data.setStudentGrade(user.getGrade());
        }
        
        repository.save(data);
    }
    
    // 获取全班所有心理数据（按提交时间倒序）
    public List<PsychologicalData> getAllClassData() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    // 删除心理数据
    public void deleteData(Integer dataId) {
        repository.deleteById(dataId);
    }
    
    // 删除旧的星级评分数据（1-5分的旧数据）
    public void deleteOldStarRatingData() {
        List<PsychologicalData> oldData = repository.findByScoreBetween(1, 5);
        repository.deleteAll(oldData);
    }
    
    public Map<String, Double> getInterventionComparison(Integer userId) {
        List<PsychologicalData> allData = repository.findByUserIdOrderByTestDateDesc(userId);
        
        double preSum = 0;
        int preCount = 0;
        double postSum = 0;
        int postCount = 0;
        
        for (PsychologicalData data : allData) {
            if (data.getCdRiscScore() != null) {
                if ("PRE_INTERVENTION".equals(data.getInterventionStatus())) {
                    preSum += data.getCdRiscScore();
                    preCount++;
                } else if ("POST_INTERVENTION".equals(data.getInterventionStatus())) {
                    postSum += data.getCdRiscScore();
                    postCount++;
                }
            }
        }
        
        Map<String, Double> result = new HashMap<>();
        result.put("preAverage", preCount > 0 ? preSum / preCount : 0);
        result.put("postAverage", postCount > 0 ? postSum / postCount : 0);
        
        return result;
    }
    
    // 获取用户的所有心理数据（带 CD-RISC 分数）- 按时间倒序
    public List<PsychologicalData> getUserDataWithCDRISC(Integer userId) {
        return repository.findByUserIdAndCdRiscScoreIsNotNullOrderByCreatedAtDesc(userId);
    }
    
    // 获取全班心理得分分布统计（用于饼图）- 按学生人数统计
    public Map<String, Object> getClassScoreDistribution() {
        List<PsychologicalData> allData = repository.findAllByOrderByTestDateDesc();
        
        // 🌟 新增：获取所有学生账号（只统计学生角色）
        List<User> allStudents = userRepository.findByRole("student");
        Set<Integer> studentIds = new HashSet<>();
        for (User student : allStudents) {
            studentIds.add(student.getId());
        }
        
        System.out.println("\n【心理得分概览】系统中有 " + allStudents.size() + " 个学生账号");
        
        // 按 user_id 分组，只取每个学生最新的一条数据
        Map<Integer, PsychologicalData> latestDataMap = new HashMap<>();
        int skippedCount = 0;
        
        for (PsychologicalData data : allData) {
            Integer userId = data.getUserId();
            if (userId != null && studentIds.contains(userId)) {  // 🌟 只处理学生账号的数据
                if (!latestDataMap.containsKey(userId)) {
                    latestDataMap.put(userId, data);
                }
            } else if (userId != null && !studentIds.contains(userId)) {
                skippedCount++;
                System.out.println("  ⚠️ 跳过非学生账号 ID=" + userId + " 的数据");
            }
        }
        
        System.out.println("✓ 共统计 " + latestDataMap.size() + " 个学生的数据（跳过 " + skippedCount + " 条非学生数据）");
        
        // 统计各分数段人数
        int score5 = 0, score4 = 0, score3 = 0, score2 = 0, score1 = 0;
        int total = 0;
        
        for (PsychologicalData data : latestDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                // 将分数转换为 5 分制（假设满分 100 分，每 20 分为一档）
                int normalizedScore = Math.min(5, Math.max(1, (int) Math.ceil(score / 20.0)));
                switch (normalizedScore) {
                    case 5: score5++; break;
                    case 4: score4++; break;
                    case 3: score3++; break;
                    case 2: score2++; break;
                    case 1: score1++; break;
                }
                total++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("score5", score5);
        result.put("score4", score4);
        result.put("score3", score3);
        result.put("score2", score2);
        result.put("score1", score1);
        result.put("total", total);
        
        // 计算百分比
        if (total > 0) {
            result.put("score5Percent", Math.round(score5 * 100.0 / total * 100) / 100.0);
            result.put("score4Percent", Math.round(score4 * 100.0 / total * 100) / 100.0);
            result.put("score3Percent", Math.round(score3 * 100.0 / total * 100) / 100.0);
            result.put("score2Percent", Math.round(score2 * 100.0 / total * 100) / 100.0);
            result.put("score1Percent", Math.round(score1 * 100.0 / total * 100) / 100.0);
        } else {
            result.put("score5Percent", 0.0);
            result.put("score4Percent", 0.0);
            result.put("score3Percent", 0.0);
            result.put("score2Percent", 0.0);
            result.put("score1Percent", 0.0);
        }
        
        return result;
    }
    
    // 获取全班干预前后对比数据（用于趋势图）- 按学生人数统计
    public Map<String, Object> getClassInterventionComparison() {
        // 🌟 新增：获取所有学生账号（只统计学生角色）
        List<User> allStudents = userRepository.findByRole("student");
        Set<Integer> studentIds = new HashSet<>();
        for (User student : allStudents) {
            studentIds.add(student.getId());
        }
        
        System.out.println("\n【情绪与能量趋势】系统中有 " + allStudents.size() + " 个学生账号");
        
        List<PsychologicalData> allData = repository.findAllByOrderByCreatedAtDesc();
        
        // 按 user_id 分组，分别取每个学生干预前和干预后的最新数据
        Map<Integer, PsychologicalData> preDataMap = new HashMap<>();
        Map<Integer, PsychologicalData> postDataMap = new HashMap<>();
        int skippedCount = 0;
        
        for (PsychologicalData data : allData) {
            Integer userId = data.getUserId();
            if (userId == null || !studentIds.contains(userId)) {
                if (userId != null) {
                    skippedCount++;
                    System.out.println("  ⚠️ 跳过非学生账号 ID=" + userId + " 的数据");
                }
                continue;
            }
            
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score == null) continue;
            
            if ("PRE_INTERVENTION".equals(data.getInterventionStatus())) {
                // 只保留最新的干预前数据
                if (!preDataMap.containsKey(userId)) {
                    preDataMap.put(userId, data);
                }
            } else if ("POST_INTERVENTION".equals(data.getInterventionStatus())) {
                // 只保留最新的干预后数据
                if (!postDataMap.containsKey(userId)) {
                    postDataMap.put(userId, data);
                }
            }
        }
        
        System.out.println("✓ 干预前统计 " + preDataMap.size() + " 人，干预后统计 " + postDataMap.size() + " 人（跳过 " + skippedCount + " 条非学生数据）");
        
        // 计算干预前平均分
        double preSum = 0;
        int preCount = preDataMap.size();
        for (PsychologicalData data : preDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                preSum += score;
            }
        }
        
        // 计算干预后平均分
        double postSum = 0;
        int postCount = postDataMap.size();
        for (PsychologicalData data : postDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                postSum += score;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("preAverage", preCount > 0 ? Math.round(preSum / preCount * 100) / 100.0 : 0);
        result.put("postAverage", postCount > 0 ? Math.round(postSum / postCount * 100) / 100.0 : 0);
        result.put("preCount", preCount);
        result.put("postCount", postCount);
        
        return result;
    }
    
    // 获取指定班级的所有学生ID
    public Set<Integer> getClassStudentIds(String classId) {
        Set<Integer> studentIds = new HashSet<>();
        if (classId == null || classId.isEmpty()) {
            return studentIds;
        }
        List<User> students = userRepository.findByClassIdAndRole(classId, "student");
        for (User student : students) {
            studentIds.add(student.getId());
        }
        return studentIds;
    }
    
    // 按班级获取所有心理数据（按提交时间倒序）
    public List<PsychologicalData> getClassDataByClassId(String classId) {
        Set<Integer> studentIds = getClassStudentIds(classId);
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PsychologicalData> allData = repository.findAllByOrderByCreatedAtDesc();
        List<PsychologicalData> filteredData = new ArrayList<>();
        for (PsychologicalData data : allData) {
            if (data.getUserId() != null && studentIds.contains(data.getUserId())) {
                filteredData.add(data);
            }
        }
        return filteredData;
    }
    
    // 获取指定班级的心理得分分布统计
    public Map<String, Object> getClassScoreDistributionByClassId(String classId) {
        Set<Integer> studentIds = getClassStudentIds(classId);
        
        if (studentIds.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("score5", 0);
            emptyResult.put("score4", 0);
            emptyResult.put("score3", 0);
            emptyResult.put("score2", 0);
            emptyResult.put("score1", 0);
            emptyResult.put("total", 0);
            emptyResult.put("score5Percent", 0.0);
            emptyResult.put("score4Percent", 0.0);
            emptyResult.put("score3Percent", 0.0);
            emptyResult.put("score2Percent", 0.0);
            emptyResult.put("score1Percent", 0.0);
            return emptyResult;
        }
        
        List<PsychologicalData> allData = repository.findAllByOrderByTestDateDesc();
        
        // 按 user_id 分组，只取每个学生最新的一条数据
        Map<Integer, PsychologicalData> latestDataMap = new HashMap<>();
        
        for (PsychologicalData data : allData) {
            Integer userId = data.getUserId();
            if (userId != null && studentIds.contains(userId)) {
                if (!latestDataMap.containsKey(userId)) {
                    latestDataMap.put(userId, data);
                }
            }
        }
        
        // 统计各分数段人数
        int score5 = 0, score4 = 0, score3 = 0, score2 = 0, score1 = 0;
        int total = 0;
        
        for (PsychologicalData data : latestDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                int normalizedScore = Math.min(5, Math.max(1, (int) Math.ceil(score / 20.0)));
                switch (normalizedScore) {
                    case 5: score5++; break;
                    case 4: score4++; break;
                    case 3: score3++; break;
                    case 2: score2++; break;
                    case 1: score1++; break;
                }
                total++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("score5", score5);
        result.put("score4", score4);
        result.put("score3", score3);
        result.put("score2", score2);
        result.put("score1", score1);
        result.put("total", total);
        
        if (total > 0) {
            result.put("score5Percent", Math.round(score5 * 100.0 / total * 100) / 100.0);
            result.put("score4Percent", Math.round(score4 * 100.0 / total * 100) / 100.0);
            result.put("score3Percent", Math.round(score3 * 100.0 / total * 100) / 100.0);
            result.put("score2Percent", Math.round(score2 * 100.0 / total * 100) / 100.0);
            result.put("score1Percent", Math.round(score1 * 100.0 / total * 100) / 100.0);
        } else {
            result.put("score5Percent", 0.0);
            result.put("score4Percent", 0.0);
            result.put("score3Percent", 0.0);
            result.put("score2Percent", 0.0);
            result.put("score1Percent", 0.0);
        }
        
        return result;
    }
    
    // 获取指定班级的干预前后对比数据
    public Map<String, Object> getClassInterventionComparisonByClassId(String classId) {
        Set<Integer> studentIds = getClassStudentIds(classId);
        
        Map<String, Object> emptyResult = new HashMap<>();
        emptyResult.put("preAverage", 0);
        emptyResult.put("postAverage", 0);
        emptyResult.put("preCount", 0);
        emptyResult.put("postCount", 0);
        
        if (studentIds.isEmpty()) {
            return emptyResult;
        }
        
        List<PsychologicalData> allData = repository.findAllByOrderByCreatedAtDesc();
        
        Map<Integer, PsychologicalData> preDataMap = new HashMap<>();
        Map<Integer, PsychologicalData> postDataMap = new HashMap<>();
        
        for (PsychologicalData data : allData) {
            Integer userId = data.getUserId();
            if (userId == null || !studentIds.contains(userId)) {
                continue;
            }
            
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score == null) continue;
            
            if ("PRE_INTERVENTION".equals(data.getInterventionStatus())) {
                if (!preDataMap.containsKey(userId)) {
                    preDataMap.put(userId, data);
                }
            } else if ("POST_INTERVENTION".equals(data.getInterventionStatus())) {
                if (!postDataMap.containsKey(userId)) {
                    postDataMap.put(userId, data);
                }
            }
        }
        
        double preSum = 0;
        int preCount = preDataMap.size();
        for (PsychologicalData data : preDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                preSum += score;
            }
        }
        
        double postSum = 0;
        int postCount = postDataMap.size();
        for (PsychologicalData data : postDataMap.values()) {
            Integer score = data.getCdRiscScore() != null ? data.getCdRiscScore() : data.getScore();
            if (score != null) {
                postSum += score;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("preAverage", preCount > 0 ? Math.round(preSum / preCount * 100) / 100.0 : 0);
        result.put("postAverage", postCount > 0 ? Math.round(postSum / postCount * 100) / 100.0 : 0);
        result.put("preCount", preCount);
        result.put("postCount", postCount);
        
        return result;
    }
}