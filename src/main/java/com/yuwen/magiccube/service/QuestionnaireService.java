package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.*;
import com.yuwen.magiccube.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class QuestionnaireService {
    
    @Autowired
    private QuestionnaireRepository questionnaireRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private OptionRepository optionRepository;
    
    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private PsychologicalDataRepository psychologicalDataRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PsychologicalDataService psychologicalDataService;
    
    // 获取所有问卷
    public List<Questionnaire> getAllQuestionnaires() {
        return questionnaireRepository.findAll();
    }
        
    // 获取 25 道题的 CD-RISC 问卷
    public Questionnaire getCDRISCQuestionnaire() {
        List<Questionnaire> all = questionnaireRepository.findAll();
        for (Questionnaire q : all) {
            if (q.getTitle().contains("25 道题") || q.getTitle().contains("CD-RISC") || q.getTitle().contains("心理弹性")) {
                return q;
            }
        }
        return null;
    }
        
    // 根据 ID 获取问卷详情（包含题目和选项）
    public Questionnaire getQuestionnaireWithDetails(Integer id) {
        Optional<Questionnaire> questionnaireOpt = questionnaireRepository.findById(id);
        if (questionnaireOpt.isPresent()) {
            Questionnaire questionnaire = questionnaireOpt.get();
            List<Question> questions = questionRepository.findByQuestionnaireId(id);
            
            // 为每个题目加载选项
            for (Question question : questions) {
                List<Option> options = optionRepository.findByQuestionId(question.getId());
                question.setOptions(options);
            }
            
            questionnaire.setQuestions(questions);
            return questionnaire;
        }
        return null;
    }
    
    // 初始化默认心理问卷 - 25 道题（CD-RISC 心理弹性量表）
    @Transactional
    public void initializeDefaultQuestionnaire() {
        // 检查是否已存在 25 道题的问卷
        List<Questionnaire> all = questionnaireRepository.findAll();
        for (Questionnaire q : all) {
            if (q.getTitle().contains("25 道题") || q.getTitle().contains("心理弹性") || q.getTitle().contains("CD-RISC")) {
                return; // 已存在，不重复创建
            }
        }
        
        // 创建默认问卷（25 道题）
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle("CD-RISC 心理弹性评估问卷（25 道题）");
        questionnaire.setDescription("Connor-Davidson 心理弹性量表，共 25 道题，用于评估个体的心理弹性和应对压力的能力。");
        questionnaire = questionnaireRepository.save(questionnaire);
        
        create25Questions(questionnaire.getId());
    }
    
    private void create25Questions(Integer questionnaireId) {
        String[] questions = {
            "我能适应变化",
            "我有亲密、安全的关系",
            "有时，命运或上帝能帮忙",
            "无论发生什么我都能应付",
            "过去的成功让我有信心面对挑战",
            "我能看到事情幽默的一面",
            "应对压力使我感到有力量",
            "经历艰难或疾病后，我往往会很快恢复",
            "事情发生总是有原因的",
            "无论结果怎样，我都会尽自己最大努力",
            "我能实现自己的目标",
            "当事情看起来没什么希望时，我不会轻易放弃",
            "我知道去哪里寻求帮助",
            "在压力下，我能够集中注意力并清晰思考",
            "我喜欢在解决问题时起带头作用",
            "我不会因失败而气馁",
            "我认为自己是个强有力的人",
            "我能做出不寻常的或艰难的决定",
            "我能处理不快乐的情绪",
            "我不得不按照预感行事",
            "我有强烈的目的感",
            "我感觉能掌控自己的生活",
            "我喜欢挑战",
            "我努力工作以达到目标",
            "我对自己的成绩感到骄傲"
        };
        
        for (int i = 0; i < questions.length; i++) {
            Question question = new Question();
            question.setQuestionnaireId(questionnaireId);
            question.setContent(questions[i]);
            question.setQuestionType("SCALE");
            question.setDimension("RESILIENCE");
            question = questionRepository.save(question);
            
            createCDRISCScaleOptions(question.getId());
        }
    }
    
    private void createCDRISCScaleOptions(Integer questionId) {
        String[] scaleLabels = {"从来不", "很少", "有时", "经常", "一直如此"};
        int[] scoreValues = {0, 1, 2, 3, 4};
        
        for (int i = 0; i < scaleLabels.length; i++) {
            Option option = new Option();
            option.setQuestionId(questionId);
            option.setContent(scaleLabels[i]);
            option.setScoreValue(scoreValues[i]);
            optionRepository.save(option);
        }
    }
    
    // 保留旧的 14 题方法（用于兼容）
    @Deprecated
    private void create14Questions(Integer questionnaireId) {
        String[] questions = {
            "我对学习充满热情",
            "我能很好地管理自己的时间",
            "我感到学习压力适中",
            "我与同学关系融洽",
            "我对未来充满信心",
            "我能积极面对困难",
            "我保持良好的学习习惯",
            "我能够集中注意力学习",
            "我对自己的表现满意",
            "我愿意主动寻求帮助",
            "我能平衡学习与休息",
            "我享受学习的过程",
            "我有明确的学习目标",
            "我能够有效缓解压力"
        };
        
        for (int i = 0; i < questions.length; i++) {
            Question question = new Question();
            question.setQuestionnaireId(questionnaireId);
            question.setContent(questions[i]);
            question.setQuestionType("SCALE");
            question.setDimension("MENTAL_HEALTH");
            question = questionRepository.save(question);
            
            createScaleOptions(question.getId(), 1, 5);
        }
    }
    
    private void createScaleOptions(Integer questionId, int startValue, int endValue) {
        String[] scaleLabels = {"非常不同意", "不同意", "一般", "同意", "非常同意"};
        
        for (int i = startValue; i <= endValue && i < scaleLabels.length; i++) {
            Option option = new Option();
            option.setQuestionId(questionId);
            option.setContent(scaleLabels[i-1]); // 调整索引以匹配 1-5 评分
            option.setScoreValue(i);
            optionRepository.save(option);
        }
    }
    
    // 保存用户答案（不删除历史数据，保留记录）
    @Transactional
    public void saveUserAnswers(Integer userId, Integer questionnaireId, Map<Integer, Object> answers, String interventionStatus) {
        System.out.println("[Service] 开始保存答案...");
        System.out.println("[Service] 用户 ID: " + userId);
        System.out.println("[Service] 问卷 ID: " + questionnaireId);
        System.out.println("[Service] 答案数量：" + answers.size());
        
        // 不删除之前的答案，保留历史记录
        // userAnswerRepository.deleteByUserIdAndQuestionnaireId(userId, questionnaireId);
        
        int totalScore = 0;
        
        for (Map.Entry<Integer, Object> entry : answers.entrySet()) {
            Integer questionId = entry.getKey();
            Object answer = entry.getValue();
            
            UserAnswer userAnswer = new UserAnswer();
            userAnswer.setUserId(userId);
            userAnswer.setQuestionnaireId(questionnaireId);
            userAnswer.setQuestionId(questionId);
            // 设置干预状态
            userAnswer.setInterventionStatus(interventionStatus);
            
            if (answer instanceof Integer) {
                userAnswer.setScaleScore((Integer) answer);
                totalScore += (Integer) answer;
                System.out.println("[Service] 保存题目 " + questionId + " 的分数：" + answer);
            } else if (answer instanceof String) {
                userAnswer.setAnswerText((String) answer);
                System.out.println("[Service] 保存题目 " + questionId + " 的文本答案：" + answer);
            }
            
            userAnswerRepository.save(userAnswer);
        }
        
        System.out.println("[Service] 总分：" + totalScore);
        System.out.println("[Service] 干预状态：" + interventionStatus);
        
        // 保存心理健康总分到 psychological_data 表，确保事务立即提交
        PsychologicalData data = new PsychologicalData();
        data.setUserId(userId);
        data.setCdRiscScore(totalScore); // 使用 cd_risc_score 字段存储总分
        data.setTestDate(java.time.LocalDate.now());
        data.setInterventionStatus(interventionStatus);
        data.setScore(totalScore);
        
        // 保存学生信息快照 (允许为 null，不强制要求有值)
        Optional<com.yuwen.magiccube.entity.User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            com.yuwen.magiccube.entity.User user = userOpt.get();
            data.setStudentName(user.getName());
            data.setStudentGender(user.getGender());
            data.setStudentAge(user.getAge() != null ? user.getAge() : 0); // 设置默认值 0
            data.setStudentGrade(user.getGrade());
        }
        
        System.out.println("[Service] 准备保存心理数据...");
        System.out.println("[Service] 数据详情：userId=" + userId + ", score=" + totalScore + ", interventionStatus=" + interventionStatus);
        psychologicalDataRepository.save(data);
        System.out.println("[Service] ✅ 心理数据已保存到 psychological_data 表");
    }
    
    public Integer calculateTotalScore(Integer userId, Integer questionnaireId) {
        List<UserAnswer> userAnswers = userAnswerRepository.findByUserIdAndQuestionnaireId(userId, questionnaireId);
        int totalScore = 0;
        
        for (UserAnswer answer : userAnswers) {
            if (answer.getScaleScore() != null) {
                totalScore += answer.getScaleScore();
            }
        }
        
        return totalScore;
    }
    
    public Map<String, Double> calculateDimensionScores(Integer userId, Integer questionnaireId) {
        List<UserAnswer> userAnswers = userAnswerRepository.findByUserIdAndQuestionnaireId(userId, questionnaireId);
        
        Map<String, List<Integer>> dimensionScores = new HashMap<>();
        Map<String, Double> averageScores = new HashMap<>();
        
        for (UserAnswer answer : userAnswers) {
            if (answer.getScaleScore() != null) {
                Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
                if (question != null && question.getDimension() != null) {
                    dimensionScores.computeIfAbsent(question.getDimension(), k -> new ArrayList<>())
                                  .add(answer.getScaleScore());
                }
            }
        }
        
        for (Map.Entry<String, List<Integer>> entry : dimensionScores.entrySet()) {
            String dimension = entry.getKey();
            List<Integer> scores = entry.getValue();
            double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            averageScores.put(dimension, average);
        }
        
        return averageScores;
    }
    
    // 根据数据 ID 获取用户的历史答题详情（包含题目、选项和用户答案）
    public Map<String, Object> getHistoryDetail(Integer dataId) {
        Optional<PsychologicalData> dataOpt = psychologicalDataRepository.findById(dataId);
        if (!dataOpt.isPresent()) {
            return null;
        }
        
        PsychologicalData data = dataOpt.get();
        Integer userId = data.getUserId();
        
        java.time.LocalDateTime createdAt = data.getCreatedAt();
        if (createdAt == null) {
            return null;
        }
        
        java.time.LocalDateTime startTime = createdAt.minusSeconds(30);
        java.time.LocalDateTime endTime = createdAt.plusSeconds(30);
        
        List<UserAnswer> userAnswers = userAnswerRepository.findByUserIdAndSubmittedAtBetweenOrderBySubmittedAtAsc(
            userId, startTime, endTime
        );
        
        System.out.println("[getHistoryDetail] dataId=" + dataId + ", userId=" + userId + ", createdAt=" + createdAt);
        System.out.println("[getHistoryDetail] 查询时间窗口: " + startTime + " ~ " + endTime);
        System.out.println("[getHistoryDetail] 找到答案数量: " + userAnswers.size());
        
        if (userAnswers.isEmpty()) {
            java.time.LocalDateTime broaderStart = createdAt.minusMinutes(2);
            java.time.LocalDateTime broaderEnd = createdAt.plusMinutes(2);
            
            userAnswers = userAnswerRepository.findByUserIdAndSubmittedAtBetweenOrderBySubmittedAtAsc(
                userId, broaderStart, broaderEnd
            );
            
            System.out.println("[getHistoryDetail] 扩大时间窗口: " + broaderStart + " ~ " + broaderEnd);
            System.out.println("[getHistoryDetail] 扩大后找到答案数量: " + userAnswers.size());
        }
        
        if (userAnswers.isEmpty()) {
            System.err.println("[getHistoryDetail] ❌ 未找到任何答案！dataId=" + dataId);
            return null;
        }
        
        Questionnaire questionnaire = getCDRISCQuestionnaire();
        if (questionnaire == null) {
            return null;
        }
        
        List<Question> questions = questionRepository.findByQuestionnaireId(questionnaire.getId());
        for (Question question : questions) {
            List<Option> options = optionRepository.findByQuestionId(question.getId());
            question.setOptions(options);
        }
        
        Map<Integer, Object> userAnswersMap = new HashMap<>();
        for (UserAnswer ua : userAnswers) {
            if (ua.getScaleScore() != null) {
                userAnswersMap.put(ua.getQuestionId(), ua.getScaleScore());
            }
        }
        
        System.out.println("[getHistoryDetail] ✅ 成功构建答案 Map，共 " + userAnswersMap.size() + " 道题");
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("questions", questions);
        result.put("userAnswers", userAnswersMap);
        
        return result;
    }

}
