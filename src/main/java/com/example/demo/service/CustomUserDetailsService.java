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
        Users user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().getRoleName()) 
            .disabled(!user.getIsActive()) 
            .accountExpired(false)
            .credentialsExpired(false)
            .accountLocked(false)
            .build();
    }
}