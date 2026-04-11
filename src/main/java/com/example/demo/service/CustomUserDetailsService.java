package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Users;
import com.example.demo.repository.UsersRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired private UsersRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm user từ database
        Users user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        
        // 2. Trả về đối tượng UserDetails tích hợp sẵn trạng thái isActive
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().getRoleName()) 
            // .disabled(true) nếu isActive = false, ngược lại enabled = true
            .disabled(!user.getIsActive()) 
            .accountExpired(false)
            .credentialsExpired(false)
            .accountLocked(false)
            .build();
    }
}