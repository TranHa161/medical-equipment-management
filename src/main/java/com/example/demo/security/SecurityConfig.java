package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng BCrypt để mã hóa mật khẩu như bạn mong muốn
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tắt CSRF để Postman có thể gửi lệnh POST/PUT/DELETE mà không bị chặn
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers("/login", "/redirect","/forgot-password", "/reset-password", "/assets/**", "/css/**", "/js/**", "/uploads/**").permitAll()
            	    .requestMatchers("/dashboard").hasRole("ADMIN")
            	    .requestMatchers("/schedules", "/schedules/**", "/requests", "/requests/**").hasAnyRole("ADMIN", "ENDUSER")
            	    .requestMatchers("/work-orders/**").hasAnyRole("ADMIN", "TECHNICIAN")
            	    .requestMatchers("/profile/**").authenticated()
            	    .anyRequest().authenticated()
            	)

            .httpBasic(Customizer.withDefaults())
            
                // ⭐ FORM LOGIN
            .formLogin(form -> form
            	    .loginPage("/login")              // GET hiển thị trang
            	    .loginProcessingUrl("/login")  // POST xử lý login
            	    .defaultSuccessUrl("/redirect", true)
            	    .failureUrl("/login?error")
            	    .permitAll()
            	)


                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                );

        return http.build();
    }
}