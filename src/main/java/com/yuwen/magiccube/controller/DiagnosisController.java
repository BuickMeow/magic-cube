package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.service.LearningDiagnosisService;
import com.yuwen.magiccube.service.PsychologicalWarningService;
import com.yuwen.magiccube.service.UserService;
import com.yuwen.magiccube.service.PsychologicalDataService;
import com.yuwen.magiccube.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@RequestMapping("/diagnosis")
public class DiagnosisController {
    
    @Autowired
    private LearningDiagnosisService learningDiagnosisService;
    
    @Autowired
    private PsychologicalWarningService psychologicalWarningService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PsychologicalDataService psychologicalDataService;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    // 个人学情诊断报告
    @GetMapping("/personal")
    public String personalDiagnosis(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<PsychologicalData> historyData = psychologicalDataService.getUserDataWithCDRISC(user.getId());
        
        Integer latestScore = null;
        if (historyData != null && !historyData.isEmpty()) {
            latestScore = historyData.get(0).getCdRiscScore();
        }

        String interventionStatus = null;
        if (historyData != null && !historyData.isEmpty()) {
            interventionStatus = historyData.get(0).getInterventionStatus();
        }

        model.addAttribute("user", user);
        model.addAttribute("historyData", historyData);
        model.addAttribute("latestScore", latestScore);
        model.addAttribute("interventionStatus", interventionStatus);
        model.addAttribute("aiSuggestionPending", true);

        return "diagnosis/personal";
    }
    
    @GetMapping("/api/ai-suggestion")
    @ResponseBody
    public Map<String, String> getAISuggestion(
            HttpSession session,
            @RequestParam Integer score,
            @RequestParam String interventionStatus) {
        
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return Map.of("error", "未登录", "suggestion", "");
        }

        try {
            CompletableFuture<String> future = deepSeekService.generateSuggestionAsync(
                    score,
                    interventionStatus,
                    user.getName()
            );
            
            String suggestion = future.join();
            
            return Map.of("suggestion", suggestion != null ? suggestion : "建议生成失败");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "error", "AI 建议生成失败",
                "suggestion", getFallbackSuggestion(score, user.getName())
            );
        }
    }
    
    // 教师端全班学情诊断报告（包含预警功能）
    @GetMapping("/class")
    public String classDiagnosis(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }
        
        List<User> warningStudents = psychologicalWarningService.getStudentsNeedWarning();
        
        List<User> allStudents = userService.getAllStudents();
        
        Map<String, Object> classReportData = learningDiagnosisService.generateClassReport();
        
        model.addAttribute("warningStudents", warningStudents);
        model.addAttribute("allStudents", allStudents);
        model.addAttribute("classReportData", classReportData);
        model.addAttribute("user", user);
        model.addAttribute("warningCount", warningStudents.size());
        
        return "diagnosis/class";
    }
    
    // 获取 AI 干预建议（新接口）
    @PostMapping("/api/ai-intervention")
    @ResponseBody
    public Map<String, String> getAIInterventionSuggestion(@RequestBody Map<String, String> requestData) {
        try {
            String summary = requestData.get("summary");
            
            // 调用 DeepSeek API 生成建议
            String aiSuggestion = deepSeekService.analyzePsychologicalState(
                null, null, null, null, null
            );
            
            // 这里可以优化，传入更详细的参数
            return Map.of("suggestion", aiSuggestion != null ? aiSuggestion : "AI 建议生成失败");
            
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("suggestion", "暂时无法提供 AI 建议，请稍后重试。");
        }
    }
    
    // 干预前后对比报告
    @GetMapping("/comparison")
    public String interventionComparison(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        LocalDate interventionDate = LocalDate.now().minusMonths(1);
        Map<String, Object> comparisonData = learningDiagnosisService
            .generateInterventionComparison(user.getId(), interventionDate);
            
        model.addAttribute("comparisonData", comparisonData);
        model.addAttribute("interventionDate", interventionDate);
        model.addAttribute("user", user);
        return "diagnosis/comparison";
    }
    
    private String getFallbackSuggestion(Integer score, String userName) {
        StringBuilder suggestion = new StringBuilder();
        
        if (score >= 80) {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，你的心理弹性得分非常优秀！");
            suggestion.append("这表明你具有很强的抗压能力和情绪调节能力。继续保持积极乐观的心态，相信你能应对各种挑战！");
        } else if (score >= 60) {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，你的心理弹性处于良好水平。");
            suggestion.append("面对压力时，你可以尝试更多放松技巧，如深呼吸、冥想等。记住，适度的压力有助于成长，但也要学会适时放松。");
        } else {
            suggestion.append("亲爱的").append(userName != null ? userName : "同学").append("，最近可能遇到了一些挑战。");
            suggestion.append("请记住，寻求帮助是勇敢的表现。可以和信任的老师、家长或心理咨询师聊聊，他们会给你专业的支持和指导。");
        }
        
        return suggestion.toString();
    }
}
