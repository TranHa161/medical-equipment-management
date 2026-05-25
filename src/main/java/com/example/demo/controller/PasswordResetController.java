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

    @GetMapping("/reset-password")
    public String showResetPage(@RequestParam("token") String token, Model model) {
        boolean isValid = tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .isPresent();

        if (!isValid) {
            return "redirect:/login?error=token_invalid";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleReset(@RequestParam("token") String token, 
                              @RequestParam("newPassword") String newPassword) {
        try {
            userService.updatePasswordAndDeleteToken(token, newPassword);
            return "redirect:/login?resetSuccess=true";
        } catch (Exception e) {
            return "redirect:/login?error=reset_failed";
        }
    }
    
    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<String> processForgotPassword(@RequestParam("username") String username, 
                                                       HttpServletRequest request) {
        try {
            String host = request.getRequestURL().toString().replace(request.getRequestURI(), "");
            
            userService.createPasswordResetTokenForUser(username, host);
            
            return ResponseEntity.ok("Link khôi phục đã được gửi vào Email của bạn.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống, vui lòng thử lại sau!");
        }
    }
}