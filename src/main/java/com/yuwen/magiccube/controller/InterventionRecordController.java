package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.InterventionRecord;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.InterventionRecordService;
import com.yuwen.magiccube.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/intervention")
public class InterventionRecordController {

    @Autowired
    private InterventionRecordService interventionRecordService;

    @Autowired
    private UserService userService;

    // 干预记录列表页（教师端）
    @GetMapping
    public String interventionList(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        if ("admin".equals(user.getRole())) {
            // 教师端：只显示自己班级学生的干预记录
            List<User> classStudents = userService.getStudentsByClassId(user.getClassId());
            List<InterventionRecord> records = interventionRecordService.getRecordsByTeacher(user.getId());
            model.addAttribute("records", records);
            model.addAttribute("classStudents", classStudents);
            model.addAttribute("user", user);
            model.addAttribute("isTeacher", true);
            return "intervention/list";
        } else {
            // 学生端：显示与自己相关的干预记录
            List<InterventionRecord> records = interventionRecordService.getRecordsByStudent(user.getId());
            model.addAttribute("records", records);
            model.addAttribute("user", user);
            model.addAttribute("isTeacher", false);
            return "intervention/list";
        }
    }

    // 查看某个学生的干预记录详情（教师端）
    @GetMapping("/student/{studentId}")
    public String studentInterventionDetail(@PathVariable Integer studentId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        if (!"admin".equals(user.getRole())) {
            return "redirect:/intervention";
        }

        User student = userService.findById(studentId);
        if (student == null) {
            return "redirect:/intervention";
        }

        // 班级隔离检查：只能看自己班级的学生
        if (student.getClassId() == null || !student.getClassId().equals(user.getClassId())) {
            return "redirect:/intervention";
        }

        List<InterventionRecord> records = interventionRecordService.getRecordsByStudentAndTeacher(studentId, user.getId());
        model.addAttribute("records", records);
        model.addAttribute("student", student);
        model.addAttribute("user", user);
        return "intervention/detail";
    }

    // 新增干预记录页面
    @GetMapping("/add")
    public String addInterventionPage(@RequestParam(required = false) Integer studentId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<User> classStudents = userService.getStudentsByClassId(user.getClassId());
        model.addAttribute("classStudents", classStudents);
        model.addAttribute("selectedStudentId", studentId);
        model.addAttribute("user", user);
        return "intervention/add";
    }

    // 提交新增干预记录
    @PostMapping("/add")
    public String addIntervention(@RequestParam Integer studentId,
                                  @RequestParam String title,
                                  @RequestParam String content,
                                  @RequestParam(required = false) String interventionType,
                                  HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }

        User student = userService.findById(studentId);
        if (student == null) {
            return "redirect:/intervention";
        }

        // 班级隔离检查
        if (student.getClassId() == null || !student.getClassId().equals(user.getClassId())) {
            return "redirect:/intervention";
        }

        interventionRecordService.addRecord(studentId, user.getId(), title, content, interventionType);
        return "redirect:/intervention/student/" + studentId;
    }

    // 删除干预记录
    @PostMapping("/delete/{id}")
    public String deleteIntervention(@PathVariable Integer id, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !"admin".equals(user.getRole())) {
            return "redirect:/login";
        }

        InterventionRecord record = interventionRecordService.getRecordById(id).orElse(null);
        if (record != null && record.getTeacherId().equals(user.getId())) {
            interventionRecordService.deleteRecord(id);
        }

        return "redirect:/intervention";
    }
}
