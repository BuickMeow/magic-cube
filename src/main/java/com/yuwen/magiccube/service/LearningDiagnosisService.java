package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.Post;
import com.yuwen.magiccube.entity.Comment;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.PsychologicalDataRepository;
import com.yuwen.magiccube.repository.PostRepository;
import com.yuwen.magiccube.repository.CommentRepository;
import com.yuwen.magiccube.repository.UserRepository;
import com.yuwen.magiccube.utils.WordCloudUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class LearningDiagnosisService {

    @Autowired
    private PsychologicalDataRepository psychologicalDataRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordCloudUtil wordCloudUtil;

    // 生成个人学情诊断报告
    public Map<String, Object> generatePersonalReport(Integer userId) {
        Map<String, Object> report = new HashMap<>();

        List<PsychologicalData> psychologicalData = psychologicalDataRepository
                .findByUserIdOrderByTestDateAsc(userId);

        if (psychologicalData == null || psychologicalData.isEmpty()) {
            psychologicalData = new ArrayList<>();
        }
        report.put("psychologicalScores", psychologicalData);

        List<Post> userPosts = postRepository.findByUserId(userId);
        if (userPosts == null) {
            userPosts = new ArrayList<>();
        }
        List<String> postContents = userPosts.stream()
                .map(Post::getContent)
                .filter(content -> content != null && !content.trim().isEmpty())
                .toList();

        Map<String, Integer> wordFrequency = wordCloudUtil.countWordFrequency(postContents);
        report.put("wordCloudData", wordFrequency);

        List<Map.Entry<String, Integer>> topWords = wordFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .toList();
        report.put("topWords", topWords);

        return report;
    }

    // 生成班级学情诊断报告
    public Map<String, Object> generateClassReport() {
        Map<String, Object> classReport = new HashMap<>();

        List<User> allStudents = userRepository.findByRole("student");

        // ========== 第一部分 & 第二部分：心理问卷数据分析与预警 ==========
        List<Map<String, Object>> studentQuestionnaireData = new ArrayList<>();
        List<Map<String, Object>> warningList = new ArrayList<>();
        List<Map<String, Object>> preInterventionWarningList = new ArrayList<>();
        List<Map<String, Object>> postInterventionWarningList = new ArrayList<>();

        double femaleNormLowerBound = 55.1;
        double maleNormLowerBound = 57.43;

        for (User student : allStudents) {
            String studentName = student.getName() != null ? student.getName() : student.getUsername();
            List<PsychologicalData> studentDataList = psychologicalDataRepository
                    .findByUserIdOrderByCreatedAtDesc(student.getId());

            PsychologicalData latestPreData = null;
            PsychologicalData latestPostData = null;

            if (studentDataList != null && !studentDataList.isEmpty()) {
                for (PsychologicalData data : studentDataList) {
                    if ("PRE_INTERVENTION".equals(data.getInterventionStatus()) && latestPreData == null) {
                        latestPreData = data;
                    } else if ("POST_INTERVENTION".equals(data.getInterventionStatus()) && latestPostData == null) {
                        latestPostData = data;
                    }
                }
            }

            Integer preScore = latestPreData != null ? latestPreData.getCdRiscScore() : null;
            Integer postScore = latestPostData != null ? latestPostData.getCdRiscScore() : null;

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("name", studentName);
            studentData.put("preScore", preScore);
            studentData.put("postScore", postScore);
            studentData.put("gender", student.getGender());
            studentData.put("threshold", student.getGender() != null && "female".equals(student.getGender())
                    ? femaleNormLowerBound : maleNormLowerBound);

            if (preScore != null && postScore != null) {
                int improvement = postScore - preScore;
                studentData.put("improvement", improvement);
                studentData.put("improvementText", (improvement > 0 ? "+" : "") + improvement);
            } else if (preScore != null || postScore != null) {
                studentData.put("improvement", null);
                studentData.put("improvementText", "-");
            } else {
                studentData.put("improvement", null);
                studentData.put("improvementText", "未填写");
            }

            studentQuestionnaireData.add(studentData);

            Double threshold = student.getGender() != null && "female".equals(student.getGender())
                    ? femaleNormLowerBound : maleNormLowerBound;

            Integer scoreToCheck = postScore != null ? postScore : preScore;

            if (scoreToCheck != null && scoreToCheck < threshold) {
                Map<String, Object> warningStudent = new HashMap<>();
                warningStudent.put("name", studentName);
                warningStudent.put("userId", student.getId());
                warningStudent.put("score", scoreToCheck);
                warningStudent.put("threshold", threshold);
                warningStudent.put("gender", student.getGender());
                warningStudent.put("hasPostScore", postScore != null);
                warningStudent.put("preDataId", latestPreData != null ? latestPreData.getId() : null);
                warningStudent.put("postDataId", latestPostData != null ? latestPostData.getId() : null);

                if (postScore != null) {
                    warningStudent.put("warningMessage", "📊 干预后得分低于阈值，建议及时关注和干预");
                    postInterventionWarningList.add(warningStudent);
                } else if (preScore != null) {
                    warningStudent.put("warningMessage", "⚠️ 干预前得分低于阈值，建议尽快干预并关注变化");
                    preInterventionWarningList.add(warningStudent);
                }
                warningList.add(warningStudent);
            }
        }

        studentQuestionnaireData.sort((s1, s2) -> {
            Integer postScore1 = (Integer) s1.get("postScore");
            Integer postScore2 = (Integer) s2.get("postScore");
            Integer preScore1 = (Integer) s1.get("preScore");
            Integer preScore2 = (Integer) s2.get("preScore");
            String name1 = (String) s1.get("name");
            String name2 = (String) s2.get("name");

            if (postScore1 != null && postScore2 != null) return postScore2.compareTo(postScore1);
            else if (postScore1 != null) return -1;
            else if (postScore2 != null) return 1;

            if (preScore1 != null && preScore2 != null) return preScore2.compareTo(preScore1);
            else if (preScore1 != null) return -1;
            else if (preScore2 != null) return 1;

            return name1.compareTo(name2);
        });

        classReport.put("studentQuestionnaireData", studentQuestionnaireData);
        classReport.put("preInterventionWarningList", preInterventionWarningList);
        classReport.put("postInterventionWarningList", postInterventionWarningList);
        classReport.put("warningList", warningList);

        // ========== 第三部分：留言板关键词（核心修复区） ==========

        // 1. 获取全班【所有】真实帖子（不再受限学生角色，避免查不到数据）
        List<String> allPostContents = postRepository.findAll().stream()
                .map(Post::getContent)
                .filter(c -> c != null && !c.trim().isEmpty())
                .toList();

        // 2. 获取全班【所有】真实评论
        List<String> allCommentContents = commentRepository.findAll().stream()
                .map(Comment::getContent)
                .filter(c -> c != null && !c.trim().isEmpty())
                .toList();

        // 3. 帖子词云 & TOP20
        Map<String, Integer> postWordFrequency = wordCloudUtil.countWordFrequency(allPostContents);
        classReport.put("postWordCloudData", postWordFrequency);

        List<Map.Entry<String, Integer>> postTopWords = postWordFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .toList();
        classReport.put("postTopWords", postTopWords);

        // 4. 评论词云 & TOP20
        Map<String, Integer> commentWordFrequency = wordCloudUtil.countWordFrequency(allCommentContents);
        classReport.put("commentWordCloudData", commentWordFrequency);

        List<Map.Entry<String, Integer>> commentTopWords = commentWordFrequency.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .toList();
        classReport.put("commentTopWords", commentTopWords);

        // ========== 第四部分：AI 动态干预建议 ==========
        StringBuilder aiPromptData = new StringBuilder();
        aiPromptData.append("班级心理数据摘要：\n");
        aiPromptData.append("1. 全班平均得分：").append(
                studentQuestionnaireData.stream()
                        .filter(d -> d.get("postScore") != null)
                        .mapToInt(d -> (Integer)d.get("postScore"))
                        .average()
                        .orElse(0.0)
        ).append(" 分\n");

        aiPromptData.append("2. 需要关注的学生人数：").append(warningList.size()).append(" 人\n");

        aiPromptData.append("3. 留言板高频词（帖子）：");
        postTopWords.forEach(entry -> aiPromptData.append(entry.getKey()).append("(").append(entry.getValue()).append("次), "));
        aiPromptData.append("\n");

        aiPromptData.append("4. 留言板高频词（评论）：");
        commentTopWords.forEach(entry -> aiPromptData.append(entry.getKey()).append("(").append(entry.getValue()).append("次), "));
        aiPromptData.append("\n");

        classReport.put("aiPromptSummary", aiPromptData.toString());

        return classReport;
    }

    // 生成干预前后对比数据
    public Map<String, Object> generateInterventionComparison(Integer userId, LocalDate interventionDate) {
        Map<String, Object> comparison = new HashMap<>();

        List<PsychologicalData> beforeData = psychologicalDataRepository
                .findByUserIdAndTestDateBeforeOrderByTestDateAsc(userId, interventionDate);

        List<PsychologicalData> afterData = psychologicalDataRepository
                .findByUserIdAndTestDateAfterOrderByTestDateAsc(userId, interventionDate);

        comparison.put("beforePsychological", beforeData);
        comparison.put("afterPsychological", afterData);

        List<Post> beforePosts = postRepository.findByUserIdAndCreatedAtBefore(userId, interventionDate.atStartOfDay().plusDays(1));
        List<Post> afterPosts = postRepository.findByUserIdAndCreatedAtAfter(userId, interventionDate.atStartOfDay());

        List<String> beforeContents = beforePosts.stream().map(Post::getContent).toList();
        List<String> afterContents = afterPosts.stream().map(Post::getContent).toList();

        comparison.put("beforeWordFreq", wordCloudUtil.countWordFrequency(beforeContents));
        comparison.put("afterWordFreq", wordCloudUtil.countWordFrequency(afterContents));

        return comparison;
    }
}