package com.yuwen.magiccube.service;

import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 初始化默认学生账号和管理员账号
    public void initializeDefaultUser() {
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setPassword("123456");
            student.setRole("student");
            student.setClassId("default");
            userRepository.save(student);
            System.out.println("默认学生账号已创建：student / 123456");
        }
        
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRole("admin");
            admin.setClassId("default");
            userRepository.save(admin);
            System.out.println("默认管理员账号已创建：admin / admin123");
        }
    }

    // 登录验证 - 不再限制用户名，只要是数据库中的用户都能登录
    public User login(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // 验证密码是否匹配
            if (password.equals(user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    // 注册新用户 - 返回注册结果
    public RegisterResult register(String username, String password, String classId, String role, String name, String gender, Integer age, String grade) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(username).isPresent()) {
            return new RegisterResult(false, "用户名已存在", null);
        }
        
        // 验证输入
        if (username == null || username.trim().isEmpty()) {
            return new RegisterResult(false, "用户名不能为空", null);
        }
        
        if (password == null || password.length() < 6) {
            return new RegisterResult(false, "密码长度不能少于 6 位", null);
        }
        
        try {
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(password);
            user.setClassId(classId != null ? classId : "default");
            user.setRole(role != null ? role : "student");
            user.setName(name);
            user.setGender(gender);
            user.setAge(age);
            user.setGrade(grade);
            
            User savedUser = userRepository.save(user);
            return new RegisterResult(true, "注册成功", savedUser);
        } catch (Exception e) {
            return new RegisterResult(false, "注册失败：" + e.getMessage(), null);
        }
    }

    // 通过用户名重置密码（忘记密码功能）
    public boolean resetPasswordByUsername(String username, String newPassword) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            
            // 验证新密码长度
            if (newPassword == null || newPassword.length() < 6) {
                return false;
            }
            
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false; // 用户名不存在
    }

    // 修改密码（需要登录）
    public boolean changePassword(Integer userId, String oldPassword, String newPassword) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // 验证旧密码
            if (!oldPassword.equals(user.getPassword())) {
                return false;
            }
            
            // 验证新密码
            if (newPassword == null || newPassword.length() < 6) {
                return false;
            }
            
            user.setPassword(newPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 修改用户名（昵称）
    public boolean changeUsername(Integer userId, String newUsername) {
        // 验证新用户名是否为空
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return false;
        }
        
        // 检查新用户名是否已被使用
        Optional<User> existingUser = userRepository.findByUsername(newUsername.trim());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            return false; // 用户名已存在
        }
        
        // 修改用户名
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setUsername(newUsername.trim());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 修改用户信息（姓名、性别、年龄等）
    public boolean updateUserInfo(Integer userId, String name, String gender, Integer age, String grade) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setName(name);
            user.setGender(gender);
            user.setAge(age);
            user.setGrade(grade);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 根据 ID 获取用户
    public User getUserById(Integer userId) {
        return userRepository.findById(userId).orElse(null);
    }

    // 保存用户信息
    public User save(User user) {
        return userRepository.save(user);
    }

    // 获取所有学生
    public List<User> getAllStudents() {
        return userRepository.findByRole("student");
    }

    // 获取所有管理员
    public List<User> getAllAdmins() {
        return userRepository.findByRole("admin");
    }

    // 删除用户
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }
    
    // 注册结果类
    public static class RegisterResult {
        private boolean success;
        private String message;
        private User user;
        
        public RegisterResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public User getUser() {
            return user;
        }
    }
}