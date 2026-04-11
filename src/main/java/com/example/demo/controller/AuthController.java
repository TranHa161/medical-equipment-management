package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    // 1. Hiển thị trang Login kèm thông báo lỗi cụ thể
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error, 
                                HttpServletRequest request, Model model) {
        if (error != null) {
            // Lấy Exception cuối cùng từ Session để kiểm tra
            Exception ex = (Exception) request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            
            if (ex instanceof DisabledException) {
                // Đây là lỗi do isActive = false mà chúng mình đã cấu hình
                model.addAttribute("loginError", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản lý!");
            } else {
                model.addAttribute("loginError", "Tên đăng nhập hoặc mật khẩu không chính xác.");
            }
        }
        return "login"; 
    }

    // 2. Chuyển hướng sau khi đăng nhập thành công
    @GetMapping("/redirect")
    public String redirectAfterLogin(Authentication auth) {
        // Kiểm tra quyền Admin
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (isAdmin) {
            return "redirect:/dashboard";
        }

        return "redirect:/profile"; // Người dùng cuối (ENDUSER) chỉ vào trang Profile
    }
}