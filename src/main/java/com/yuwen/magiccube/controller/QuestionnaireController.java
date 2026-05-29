package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.Questionnaire;
import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.QuestionnaireService;
import com.yuwen.magiccube.service.PsychologicalDataService;
import com.yuwen.magiccube.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/questionnaire")
public class QuestionnaireController {
    
    @Autowired
    private QuestionnaireService questionnaireService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PsychologicalDataService psychologicalDataService;
    
    // 问卷首页 - 显示可用问卷列表
    @GetMapping
    public String questionnaireList(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 初始化默认问卷（如果还没有的话）
        questionnaireService.initializeDefaultQuestionnaire();
        
        model.addAttribute("questionnaires", questionnaireService.getAllQuestionnaires());
        model.addAttribute("user", user);
        return "questionnaire/list";
    }
    
    // 直接开始问卷（从首页或能量监测页进入）
    @GetMapping("/start")
    public String startQuestionnaire(HttpSession session, 
                                     @RequestParam(required = false) String intervention,
                                     Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 初始化默认问卷（如果还没有的话）
        questionnaireService.initializeDefaultQuestionnaire();
        
        // 获取 25 道题的 CD-RISC 问卷
        Questionnaire questionnaire = questionnaireService.getCDRISCQuestionnaire();
        if (questionnaire == null) {
            return "redirect:/questionnaire";
        }
        
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("user", user);
        model.addAttribute("needInfo", user.getGender() == null || user.getAge() == null || user.getGrade() == null);
        
        // 如果是从能量监测页点击"干预后评估"进入，自动选择干预后状态
        if ("post".equals(intervention)) {
            model.addAttribute("defaultInterventionStatus", "POST_INTERVENTION");
        } else {
            model.addAttribute("defaultInterventionStatus", "PRE_INTERVENTION");
        }
        
        return "questionnaire/take";
    }
    
    // 开始答题页面
    @GetMapping("/take/{id}")
    public String takeQuestionnaire(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        Questionnaire questionnaire = questionnaireService.getQuestionnaireWithDetails(id);
        if (questionnaire == null) {
            return "redirect:/questionnaire";
        }
        
        model.addAttribute("questionnaire", questionnaire);
        model.addAttribute("user", user);
        model.addAttribute("needInfo", user.getGender() == null || user.getAge() == null || user.getGrade() == null);
        return "questionnaire/take";
    }
    
    
    @PostMapping("/submit/{id}")
    public String submitQuestionnaire(@PathVariable Integer id, 
                                    @RequestParam Map<String, String> answers,
                                    @RequestParam(required = false) String interventionStatus,
                                    @RequestParam(required = false) String name,
                                    @RequestParam(required = false) String gender,
                                    @RequestParam(required = false) Integer age,
                                    @RequestParam(required = false) String grade,
                                    HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 打印接收到的参数，用于调试
        System.out.println("========== 问卷提交开始 ==========");
        System.out.println("问卷 ID: " + id);
        System.out.println("用户 ID: " + user.getId());
        System.out.println("姓名：" + name);
        System.out.println("性别：" + gender);
        System.out.println("年龄：" + age);
        System.out.println("年级：" + grade);
        System.out.println("干预状态：" + interventionStatus);
        System.out.println("答案数量：" + answers.size());
        System.out.println("所有答案：" + answers);
        
        // 保存学生基本信息
        if (name != null && !name.isEmpty() && gender != null && !gender.isEmpty() 
            && age != null && grade != null && !grade.isEmpty()) {
            user.setName(name);
            user.setGender(gender);
            user.setAge(age);
            user.setGrade(grade);
            userService.save(user);
            System.out.println("用户信息已更新");
        }
        
        // 设置默认干预状态
        if (interventionStatus == null || interventionStatus.isEmpty()) {
            interventionStatus = "PRE_INTERVENTION";
        }
        
        // 解析答案并计算总分
        Map<Integer, Object> formattedAnswers = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            if (entry.getKey().startsWith("question_")) {
                try {
                    Integer questionId = Integer.parseInt(entry.getKey().substring(9));
                    Integer answerValue = Integer.parseInt(entry.getValue());
                    formattedAnswers.put(questionId, answerValue);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            // 保存答案和分数
            questionnaireService.saveUserAnswers(user.getId(), id, formattedAnswers, interventionStatus);
            System.out.println("问卷提交成功，用户 ID: " + user.getId() + ", 问卷 ID: " + id);
            System.out.println("保存的答案数量：" + formattedAnswers.size());
            System.out.println("========== 问卷提交结束 ==========");
        } catch (Exception e) {
            System.err.println("问卷提交失败：" + e.getMessage());
            e.printStackTrace();
            return "redirect:/questionnaire/take/" + id + "?error=1";
        }
        
        // 跳转到能量监测窗口，显示评分
        System.out.println("重定向到能量监测页面：/energy");
        return "redirect:/energy";
    }
    
    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        Integer totalScore = questionnaireService.calculateTotalScore(user.getId(), id);
        Map<String, Double> dimensionScores = questionnaireService.calculateDimensionScores(user.getId(), id);
        
        model.addAttribute("totalScore", totalScore);
        model.addAttribute("dimensionScores", dimensionScores);
        model.addAttribute("user", user);
        model.addAttribute("norm", getNorm(user.getGender()));
        
        return "questionnaire/result";
    }
    
    @GetMapping("/history/{dataId}")
    public String viewHistoryDetail(@PathVariable Integer dataId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        Map<String, Object> detail = questionnaireService.getHistoryDetail(dataId);
        if (detail == null) {
            return "redirect:/questionnaire";
        }
        
        model.addAttribute("detail", detail);
        model.addAttribute("user", user);
        
        return "questionnaire/history-detail";
    }
    
    // 🌟 新增：教师端查看学生详细问卷（支持权限验证）
    @GetMapping("/admin/history/{dataId}")
    public String viewStudentHistoryDetail(@PathVariable Integer dataId, 
                                          @RequestParam(required = false) Integer studentId,
                                          HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        // 验证教师权限
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            return "redirect:/login";
        }
        
        // 获取问卷详情
        Map<String, Object> detail = questionnaireService.getHistoryDetail(dataId);
        if (detail == null) {
            return "redirect:/energy/admin";
        }
        
        // 如果是从预警名单跳转，验证是否是查看该学生的数据
        if (studentId != null) {
            PsychologicalData data = psychologicalDataService.getRepository().findById(dataId).orElse(null);
            if (data != null && !data.getUserId().equals(studentId)) {
                // 数据 ID 和学生 ID 不匹配，拒绝访问
                return "redirect:/diagnosis/class?error=unauthorized";
            }
        }
        
        model.addAttribute("detail", detail);
        model.addAttribute("user", currentUser);
        model.addAttribute("isTeacherView", true); // 标记为教师端查看
        
        return "questionnaire/history-detail";
    }
    
    private NormData getNorm(String gender) {
        // 25 道题的 CD-RISC 常模参考值（满分 100 分）
        // 根据 Connor & Davidson (2003) 的原始研究
        if ("female".equals(gender)) {
            return new NormData(76.5, 15.2);  // 女性平均约 76.5 分
        } else {
            return new NormData(80.2, 14.8);  // 男性平均约 80.2 分
        }
    }
    
    public static class NormData {
        private double mean;
        private double stdDev;
        
        public NormData(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
        
        public double getMean() { return mean; }
        public double getStdDev() { return stdDev; }
        public double getLowerBound() { return mean - stdDev; }
        public double getUpperBound() { return mean + stdDev; }
    }
}
