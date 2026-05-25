package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.UserSaveRequest;
import com.example.demo.dto.UsersResponseDTO;
import com.example.demo.service.UsersService;

@Controller
@RequestMapping("/users")
public class UsersController {

    @Autowired
    private UsersService usersService;

    @GetMapping
    public String listUsers(Model model, 
                            @RequestParam(required = false) String keyword) {
        
        List<UsersResponseDTO> users = (keyword == null || keyword.isEmpty())
                ? usersService.findAllUsers()
                : usersService.searchUsers(keyword);
        
        model.addAttribute("userList", users);
        model.addAttribute("keyword", keyword);
        
        return "users"; 
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        UsersResponseDTO user = usersService.getUserById(id);
        model.addAttribute("user", user);
        
        model.addAttribute("roles", usersService.getAllRoles());
        return "users-form"; 
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        UserSaveRequest user = new UserSaveRequest();
        model.addAttribute("user", user);
        model.addAttribute("roles", usersService.getAllRoles());
        return "users-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> saveUser(@RequestBody UserSaveRequest dto) {
        try {
            UsersResponseDTO saved = usersService.saveUser(dto);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}