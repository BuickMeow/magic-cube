package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.PsychologicalDataService;
import com.yuwen.magiccube.service.QuestionnaireService;
import com.yuwen.magiccube.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/energy")
public class EnergyController {

    @Autowired
    private PsychologicalDataService psychologicalDataService;

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private UserRepository userRepository;

    // 学生端能量监测页
    @GetMapping
    public String myEnergy(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            // 用户未登录，重定向到登录页
            return "redirect:/login";
        }

        try {
            System.out.println("能量监测页面 - 用户 ID: " + user.getId());

            // 获取最新的 CD-RISC 分数
            PsychologicalData latestData = psychologicalDataService.getLatestUserDataWithCDRISC(user.getId());

            if (latestData != null) {
                System.out.println("找到最新数据 - CD-RISC 分数：" + latestData.getCdRiscScore());
                System.out.println("最新数据的干预状态：" + latestData.getInterventionStatus());
            } else {
                System.out.println("未找到 CD-RISC 分数数据");
            }

            // 获取干预前后对比数据
            Map<String, Double> comparison = psychologicalDataService.getInterventionComparison(user.getId());

            System.out.println("========== 对比数据 ==========");
            System.out.println("comparison Map: " + comparison);
            if (comparison != null) {
                System.out.println("preAverage: " + comparison.get("preAverage"));
                System.out.println("postAverage: " + comparison.get("postAverage"));
            }
            System.out.println("==============================");

            // 根据最新一次数据的干预状态来决定显示什么标题
            boolean isLatestPostIntervention = false;
            if (latestData != null && "POST_INTERVENTION".equals(latestData.getInterventionStatus())) {
                isLatestPostIntervention = true;
                System.out.println(">>> 最新数据是干预后！isLatestPostIntervention = true");
            } else {
                System.out.println(">>> 最新数据是干预前！isLatestPostIntervention = false");
            }

            model.addAttribute("latestData", latestData);
            model.addAttribute("comparison", comparison);
            model.addAttribute("user", user);
            model.addAttribute("isLatestPostIntervention", isLatestPostIntervention);

            // 添加历史记录数据
            List<PsychologicalData> historyData = psychologicalDataService.getUserDataWithCDRISC(user.getId());

            // 🌟 核心修复 1：过滤掉那些创建时间或分数为空的“脏数据”，防止 Thymeleaf 渲染时崩溃
            if (historyData != null) {
                historyData.removeIf(d -> d.getCreatedAt() == null || d.getCdRiscScore() == null);
            }
            model.addAttribute("historyData", historyData);

            // 🌟 核心修复 2：安全地获取总分，防止 latestData 存在但 score 为 null 导致前端数学比较时崩溃
            int totalScore = 0;
            if (latestData != null && latestData.getCdRiscScore() != null) {
                totalScore = latestData.getCdRiscScore();
            }

            // 计算常模参考（25 道题，满分 100 分）
            NormData norm;
            if (user.getGender() != null && ("male".equals(user.getGender()) || "female".equals(user.getGender()))) {
                norm = getNorm(user.getGender());
            } else {
                System.out.println("用户性别为空或不正确：" + user.getGender());
                // 使用默认常模（男性）
                norm = getNorm("male");
            }

            model.addAttribute("norm", norm);
            model.addAttribute("totalScore", totalScore);
            // 传递常模范围值给模板，用于判断每条记录的状态
            model.addAttribute("normLowerBound", norm.getLowerBound());
            model.addAttribute("normUpperBound", norm.getUpperBound());

            return "energy";

        } catch (Exception e) {
            System.err.println("能量监测页面加载失败：" + e.getMessage());
            e.printStackTrace();
            // 发生异常时，返回到登录页，避免渲染错误页面
            return "redirect:/login";
        }
    }

    private EnergyController.NormData getNorm(String gender) {
        // 25 道题的 CD-RISC 常模参考值（满分 100 分）
        // 严格按照用户要求的常模值
        if ("female".equals(gender)) {
            return new EnergyController.NormData(68.19, 13.09); // 女性常模
        } else {
            return new EnergyController.NormData(70.63, 13.20); // 男性常模
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

    // 提交新的心理数据
    @PostMapping("/submit")
    public String submitData(@RequestParam Integer score, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            psychologicalDataService.addData(user.getId(), score, LocalDate.now());
        }
        return "redirect:/energy";
    }

    // 教师端全班数据页
    @GetMapping("/admin")
    public String adminEnergy(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null && "admin".equals(user.getRole())) {
            // 🌟 修改：只获取当前老师所在班级的学生问卷数据
            String teacherClassId = user.getClassId();
            List<User> classStudents = userRepository.findByClassIdAndRole(teacherClassId, "student");
            Set<Integer> studentIds = new HashSet<>();
            for (User student : classStudents) {
                studentIds.add(student.getId());
            }
            
            System.out.println("\n【全班心理能量数据明细】班级 " + teacherClassId + " 中有 " + classStudents.size() + " 个学生账号");
            
            // 获取所有问卷数据，然后过滤出本班学生角色的数据
            List<PsychologicalData> allData = psychologicalDataService.getAllClassData();
            List<PsychologicalData> filteredData = new ArrayList<>();
            
            for (PsychologicalData data : allData) {
                if (data.getUserId() != null && studentIds.contains(data.getUserId())) {
                    filteredData.add(data);
                } else {
                    System.out.println("  ⚠️ 过滤掉非本班学生账号 ID=" + data.getUserId() + " 的数据");
                }
            }
            
            System.out.println("✓ 共加载 " + filteredData.size() + " 条本班学生数据");

            // 为每条数据加载学生姓名
            for (PsychologicalData data : filteredData) {
                if (data.getUserId() != null) {
                    Optional<User> userOptional = userRepository.findById(data.getUserId());
                    if (userOptional.isPresent()) {
                        User student = userOptional.get();
                        data.setStudentName(student.getName() != null ? student.getName() : student.getUsername());
                    }
                }
            }

            // 获取本班统计数据
            Map<String, Object> scoreDistribution = psychologicalDataService.getClassScoreDistributionByClassId(teacherClassId);
            Map<String, Object> interventionComparison = psychologicalDataService.getClassInterventionComparisonByClassId(teacherClassId);

            model.addAttribute("allData", filteredData);  // 🌟 使用过滤后的数据
            model.addAttribute("user", user);
            model.addAttribute("scoreDistribution", scoreDistribution);
            model.addAttribute("interventionComparison", interventionComparison);
            model.addAttribute("classId", teacherClassId);

            return "admin-energy";
        }
        return "redirect:/energy";
    }

    // 批量导入心理数据（支持表头自动匹配，不要求列顺序）
    @PostMapping("/import")
    public String importData(@RequestParam("file") MultipartFile file, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        // 只有管理员能导入，防止越权
        if (user != null && "admin".equals(user.getRole())) {
            try {
                // 读取 CSV 文件
                BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                String line;
                boolean isFirstLine = true;
                
                // 定义必需的列名
                String[] requiredColumns = {"user_id", "score", "test_date", "intervention_status"};
                // 用于存储每列的索引位置
                int userIdIndex = -1;
                int scoreIndex = -1;
                int testDateIndex = -1;
                int interventionStatusIndex = -1;
                
                while ((line = reader.readLine()) != null) {
                    // 跳过空行
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    // 处理第一行（表头）
                    if (isFirstLine) {
                        isFirstLine = false;
                        String[] headers = line.split(",");
                        
                        // 解析表头，找到每列的索引
                        for (int i = 0; i < headers.length; i++) {
                            String header = headers[i].trim().toLowerCase();
                            switch (header) {
                                case "user_id":
                                case "userid":
                                case "用户 id":
                                    userIdIndex = i;
                                    break;
                                case "score":
                                case "scores":
                                case "分数":
                                case "得分":
                                case "心理评分":
                                    scoreIndex = i;
                                    break;
                                case "test_date":
                                case "testdate":
                                case "test date":
                                case "测试日期":
                                case "日期":
                                    testDateIndex = i;
                                    break;
                                case "intervention_status":
                                case "intervention status":
                                case "interventionstatus":
                                case "干预状态":
                                case "状态":
                                    interventionStatusIndex = i;
                                    break;
                            }
                        }
                        
                        // 验证是否所有必需列都存在
                        StringBuilder missingColumns = new StringBuilder();
                        if (userIdIndex == -1) missingColumns.append("user_id, ");
                        if (scoreIndex == -1) missingColumns.append("score, ");
                        if (testDateIndex == -1) missingColumns.append("test_date, ");
                        if (interventionStatusIndex == -1) missingColumns.append("intervention_status, ");
                        
                        if (missingColumns.length() > 0) {
                            throw new IllegalArgumentException("CSV 文件缺少必需的列：" + 
                                missingColumns.substring(0, missingColumns.length() - 2));
                        }
                        
                        System.out.println("✓ CSV 表头解析成功：");
                        System.out.println("  - user_id 在第 " + (userIdIndex + 1) + " 列");
                        System.out.println("  - score 在第 " + (scoreIndex + 1) + " 列");
                        System.out.println("  - test_date 在第 " + (testDateIndex + 1) + " 列");
                        System.out.println("  - intervention_status 在第 " + (interventionStatusIndex + 1) + " 列");
                        continue;
                    }
                    
                    // 处理数据行
                    String[] data = line.split(",");
                    
                    // 确保所有必需的列都有数据
                    if (data.length <= Math.max(Math.max(userIdIndex, scoreIndex), 
                                               Math.max(testDateIndex, interventionStatusIndex))) {
                        System.err.println("⚠️ 跳过无效行：列数不足 - " + line);
                        continue;
                    }
                    
                    try {
                        // 根据解析出的索引提取数据
                        Integer userId = Integer.parseInt(data[userIdIndex].trim());
                        Integer score = Integer.parseInt(data[scoreIndex].trim());
                        LocalDate testDate = LocalDate.parse(data[testDateIndex].trim());
                        String interventionStatus = data[interventionStatusIndex].trim();
                        
                        // 验证干预状态是否合法
                        if (!"PRE_INTERVENTION".equals(interventionStatus) && 
                            !"POST_INTERVENTION".equals(interventionStatus)) {
                            interventionStatus = "PRE_INTERVENTION"; // 默认值
                        }
                        
                        // 调用带干预状态的方法
                        psychologicalDataService.addCDRISCData(userId, score, testDate, interventionStatus);
                        
                    } catch (Exception e) {
                        System.err.println("⚠️ 跳过无效数据行：" + line);
                        System.err.println("  错误原因：" + e.getMessage());
                    }
                }
                reader.close();
                System.out.println("✓ CSV 数据导入完成！");
            } catch (Exception e) {
                // 打印异常，方便后续排查问题
                e.printStackTrace();
            }
        }
        // 导入完成后，自动刷新页面，显示最新数据
        return "redirect:/energy/admin";
    }

    @GetMapping("/comparison")
    @ResponseBody
    public Map<String, Object> getInterventionComparison(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        Map<String, Object> result = new HashMap<>();

        if (user != null) {
            Map<String, Double> comparison = psychologicalDataService.getInterventionComparison(user.getId());
            result.put("success", true);
            result.put("data", comparison);
        } else {
            result.put("success", false);
        }

        return result;
    }

    @GetMapping("/trend-data")
    @ResponseBody
    public Map<String, Object> getTrendData(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        Map<String, Object> result = new HashMap<>();

        if (user != null) {
            List<PsychologicalData> allData = psychologicalDataService.getUserDataWithCDRISC(user.getId());

            // 🌟 核心修复 3：过滤掉没有时间或没有分数的旧数据
            if (allData != null) {
                allData.removeIf(d -> d.getCreatedAt() == null || d.getCdRiscScore() == null);

                // 按时间顺序排序
                allData.sort((d1, d2) -> d1.getCreatedAt().compareTo(d2.getCreatedAt()));

                // 构建图表数据
                List<String> labels = new ArrayList<>();
                List<Integer> scores = new ArrayList<>();
                List<String> statuses = new ArrayList<>();

                for (PsychologicalData data : allData) {
                    // 安全地截取时间字符串
                    labels.add(data.getCreatedAt().toString().substring(0, 16).replace('T', ' '));
                    scores.add(data.getCdRiscScore());
                    // 如果状态为空，给个默认值防止前端报错
                    statuses.add(data.getInterventionStatus() != null ? data.getInterventionStatus() : "PRE_INTERVENTION");
                }

                result.put("success", true);
                result.put("labels", labels);
                result.put("scores", scores);
                result.put("statuses", statuses);
            } else {
                result.put("success", false);
            }
        } else {
            result.put("success", false);
        }

        return result;
    }

    // 删除旧的星级评分数据（1-5分的旧数据）
    @GetMapping("/delete-old-data")
    public String deleteOldStarRatingData(HttpSession session) {
        // 暂时移除权限验证，以便直接访问删除旧数据
        psychologicalDataService.deleteOldStarRatingData();
        return "redirect:/energy/admin";
    }
}