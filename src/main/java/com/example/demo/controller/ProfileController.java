package com.example.demo.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.Users;
import com.example.demo.repository.UsersRepository;
import com.example.demo.service.UsersService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired private UsersService userService;
    @Autowired private UsersRepository userRepository;
    
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("")
    public String showProfile(Model model, Authentication auth) {
        Users user = userRepository.findByUsername(auth.getName()).get();
        model.addAttribute("user", user);
        return "profile"; 
    }
    
    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam("phone") String phone,
                                @RequestParam("email") String email,
                                @RequestParam(value = "file", required = false) MultipartFile file, // ⭐ Tên "file" phải khớp HTML
                                Principal principal, 
                                RedirectAttributes ra) {
        try {
            String username = principal.getName();
            
            if (file != null && !file.isEmpty()) {
                System.out.println("Nhận được file: " + file.getOriginalFilename());
            } else {
                System.out.println("File gửi lên bị NULL hoặc RỖNG!");
            }

            userService.updateUserProfile(username, fullName, phone, email, file);
            ra.addFlashAttribute("info_success", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("info_error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String updatePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwd_error", "Mật khẩu mới và xác nhận không khớp!");
            return "redirect:/profile";
        }

        Users user = userRepository.findByUsername(auth.getName()).get();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            ra.addFlashAttribute("pwd_error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/profile";
        }

        try {
            userService.changePassword(auth.getName(), oldPassword, newPassword);
            ra.addFlashAttribute("pwd_success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("pwd_error", e.getMessage());
        }

        return "redirect:/profile";
    }

}