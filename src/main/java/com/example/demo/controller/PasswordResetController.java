package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PasswordResetController {

    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private UsersService userService;

    // 1. Hiển thị trang nhập mật khẩu mới
    @GetMapping("/reset-password")
    public String showResetPage(@RequestParam("token") String token, Model model) {
        // Kiểm tra token có tồn tại và còn hạn hay không
        boolean isValid = tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .isPresent();

        if (!isValid) {
            // Nếu không hợp lệ, quay về login với thông báo lỗi
            return "redirect:/login?error=token_invalid";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // 2. Xử lý lưu mật khẩu mới
    @PostMapping("/reset-password")
    public String handleReset(@RequestParam("token") String token, 
                              @RequestParam("newPassword") String newPassword) {
        try {
            userService.updatePasswordAndDeleteToken(token, newPassword);
            // Thành công thì quay về login kèm thông báo xanh
            return "redirect:/login?resetSuccess=true";
        } catch (Exception e) {
            // Nếu có lỗi phát sinh (ví dụ token vừa hết hạn)
            return "redirect:/login?error=reset_failed";
        }
    }
    
 // Trong PasswordResetController.java
    @PostMapping("/forgot-password")
    @ResponseBody // Trả về chuỗi văn bản thay vì một trang HTML
    public ResponseEntity<String> processForgotPassword(@RequestParam("username") String username, 
                                                       HttpServletRequest request) {
        try {
            // 1. Lấy Host name (ví dụ: http://localhost:8080) để tạo Link trong Email
            String host = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            
            // 2. Gọi Service xử lý (Tạo token, Lưu DB, Gửi Mail)
            userService.createPasswordResetTokenForUser(username, host);
            
            return ResponseEntity.ok("Link khôi phục đã được gửi vào Email của bạn.");
        } catch (RuntimeException e) {
            // Trả về lỗi nếu không tìm thấy Username hoặc User chưa có Email
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống, vui lòng thử lại sau!");
        }
    }
}