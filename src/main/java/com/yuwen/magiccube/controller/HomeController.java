package com.yuwen.magiccube.controller;

import com.yuwen.magiccube.entity.User;
import com.yuwen.magiccube.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    // 首页 - 根据角色跳转到不同页面
    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            model.addAttribute("user", user);
        }
        return "index";
    }

    // 登录页
    // ... existing code ...

    // 登录页
    @GetMapping("/login")
    public String loginPage(Model model, @CookieValue(value = "rememberUsername", defaultValue = "") String rememberUsername) {
        userService.initializeDefaultUser();
        if (!rememberUsername.isEmpty()) {
            model.addAttribute("rememberedUsername", rememberUsername);
        }
        return "login";
    }

    // 登录处理 - 根据角色跳转到不同页面
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(value = "remember", required = false) String remember,
                        HttpSession session,
                        Model model,
                        HttpServletResponse response) {
        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("currentUser", user);

            // 如果勾选了"记住我"，保存用户名到 Cookie
            if ("on".equals(remember)) {
                Cookie usernameCookie = new Cookie("rememberUsername", username);
                usernameCookie.setMaxAge(7 * 24 * 60 * 60); // 7 天
                usernameCookie.setPath("/");
                response.addCookie(usernameCookie);
            } else {
                // 如果没有勾选，删除 Cookie
                Cookie usernameCookie = new Cookie("rememberUsername", null);
                usernameCookie.setMaxAge(0);
                usernameCookie.setPath("/");
                response.addCookie(usernameCookie);
            }

            // 教师和学生都跳转到首页
            return "redirect:/";
        }
        model.addAttribute("error", "用户名或密码错误");
        return "login";
    }

// ... existing code ...


    // 注册页面
    @GetMapping("/register")
    public String registerPage(Model model) {
        return "register";
    }

    // 注册处理
    @PostMapping("/register")
    public String register(@RequestParam String username, 
                          @RequestParam String password, 
                          @RequestParam String confirmPassword,
                          @RequestParam String role,
                          @RequestParam(required = false) String classId,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) String gender,
                          @RequestParam(required = false) Integer age,
                          @RequestParam(required = false) String grade,
                          HttpSession session,
                          Model model) {
        
        // 验证两次密码是否一致
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致");
            model.addAttribute("username", username);
            model.addAttribute("role", role);
            return "register";
        }
        
        // 调用服务层注册
        UserService.RegisterResult result = userService.register(username, password, classId, role, name, gender, age, grade);
        
        if (result.isSuccess()) {
            // 注册成功，自动登录
            User user = result.getUser();
            session.setAttribute("currentUser", user);
            model.addAttribute("success", "注册成功！");
            
            // 教师和学生都跳转到首页
            return "redirect:/";
        } else {
            // 注册失败，显示错误信息
            model.addAttribute("error", result.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("role", role);
            return "register";
        }
    }

    // 忘记密码页面
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // 忘记密码处理 - 重置密码
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String username,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        
        // 验证两次密码是否一致
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致");
            return "forgot-password";
        }
        
        // 调用服务层重置密码
        boolean success = userService.resetPasswordByUsername(username, newPassword);
        
        if (success) {
            model.addAttribute("success", "密码重置成功！请登录");
            return "forgot-password";
        } else {
            model.addAttribute("error", "用户名不存在或密码格式不正确");
            return "forgot-password";
        }
    }

    // 修改密码页面（需要登录）
    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "change-password";
    }

    // 修改密码处理（需要登录）
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmNewPassword,
                                HttpSession session,
                                Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        // 验证两次新密码是否一致
        if (!newPassword.equals(confirmNewPassword)) {
            model.addAttribute("error", "两次输入的新密码不一致");
            return "change-password";
        }
        
        // 调用服务层修改密码
        boolean success = userService.changePassword(user.getId(), oldPassword, newPassword);
        
        if (success) {
            // 修改成功，更新 session 中的用户密码（保持登录状态）
            user.setPassword(newPassword);
            session.setAttribute("currentUser", user);
            
            model.addAttribute("success", "密码修改成功！");
            return "change-password";
        } else {
            model.addAttribute("error", "原密码错误或新密码长度不足 6 位");
            return "change-password";
        }
    }

    // 修改个人信息页面（包括昵称、姓名等）
    @GetMapping("/profile")
    public String profilePage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "profile";
    }

    // 修改个人信息处理
    @PostMapping("/profile")
    public String updateProfile(@RequestParam(value = "username", required = false) String username,
                               @RequestParam(value = "name", required = false) String name,
                               @RequestParam(value = "gender", required = false) String gender,
                               @RequestParam(value = "age", required = false) String ageStr,
                               @RequestParam(value = "grade", required = false) String grade,
                               HttpSession session,
                               Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            System.out.println("========== 开始保存个人资料 ==========");
            System.out.println("用户 ID: " + user.getId());
            System.out.println("新用户名：" + username);
            System.out.println("姓名：" + name);
            System.out.println("性别：" + gender);
            System.out.println("年龄字符串：" + ageStr);
            System.out.println("年级：" + grade);
            
            // 如果修改了用户名
            if (username != null && !username.trim().isEmpty() && !username.equals(user.getUsername())) {
                System.out.println("检测到用户名修改，原用户名：" + user.getUsername());
                boolean success = userService.changeUsername(user.getId(), username);
                if (success) {
                    user.setUsername(username.trim());
                    System.out.println("用户名修改成功：" + username.trim());
                } else {
                    System.out.println("用户名修改失败：已存在或格式不正确");
                    model.addAttribute("error", "用户名已存在或格式不正确");
                    model.addAttribute("user", user);
                    return "profile";
                }
            }
            
            // 处理年龄（可能为空）
            Integer age = null;
            if (ageStr != null && !ageStr.trim().isEmpty()) {
                try {
                    age = Integer.parseInt(ageStr.trim());
                    System.out.println("年龄解析成功：" + age);
                } catch (NumberFormatException e) {
                    System.out.println("年龄解析失败：" + ageStr);
                    // 忽略无效的年龄
                }
            }
            
            // 修改其他个人信息
            System.out.println("准备更新用户信息...");
            user.setName(name);
            user.setGender(gender);
            user.setAge(age);
            user.setGrade(grade);
            
            System.out.println("保存到数据库...");
            userService.save(user);
            System.out.println("保存成功！");
            
            // 更新 session
            session.setAttribute("currentUser", user);
            System.out.println("Session 已更新");
            
            model.addAttribute("success", "个人信息修改成功！");
            model.addAttribute("user", user);
            
        } catch (Exception e) {
            System.err.println("保存失败！异常信息：");
            e.printStackTrace();
            model.addAttribute("error", "保存失败：" + e.getMessage());
            model.addAttribute("user", user);
        }
        
        System.out.println("========== 个人资料保存结束 ==========");
        return "profile";
    }

    // 登出
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}