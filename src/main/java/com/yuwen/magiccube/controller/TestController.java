package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.PsychologicalData;
import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.PsychologicalDataRepository;
import com.yuwen.magiccube.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private PsychologicalDataRepository psychologicalDataRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/db")
    public Map<String, Object> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 测试用户表
            List<User> users = userRepository.findAll();
            result.put("userCount", users.size());
            result.put("users", users);
            
            // 测试心理数据表
            List<PsychologicalData> data = psychologicalDataRepository.findAll();
            result.put("psychologicalDataCount", data.size());
            result.put("psychologicalData", data);
            
            // 尝试插入一条测试数据
            PsychologicalData testData = new PsychologicalData();
            testData.setUserId(1);
            testData.setScore(50);
            testData.setTestDate(LocalDate.now());
            testData.setInterventionStatus("TEST");
            testData.setStudentName("测试用户");
            psychologicalDataRepository.save(testData);
            
            result.put("testInsert", "success");
            result.put("message", "数据库连接正常，测试数据已插入");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("message", "数据库连接失败");
            e.printStackTrace();
        }
        
        return result;
    }
    
    @GetMapping("/fix-db")
    public Map<String, Object> fixDatabase() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 执行 SQL 修复数据库表结构
            javax.sql.DataSource dataSource = null;
            
            // 使用 native SQL 修改表结构
            String[] sqls = {
                "ALTER TABLE users MODIFY COLUMN age INT NULL",
                "ALTER TABLE users MODIFY COLUMN gender VARCHAR(255) NULL",
                "ALTER TABLE users MODIFY COLUMN grade VARCHAR(255) NULL",
                "ALTER TABLE users MODIFY COLUMN name VARCHAR(255) NULL"
            };
            
            result.put("sql", sqls);
            result.put("message", "请在 MySQL 客户端执行以上 SQL 语句来修复表结构");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
}
