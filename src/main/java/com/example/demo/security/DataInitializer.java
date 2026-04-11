package com.example.demo.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Roles;
import com.example.demo.model.Users;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UsersRepository;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UsersRepository usersRepository,
            RolesRepository rolesRepository,
            PasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        System.out.println(">>> DataInitializer RUNNING <<<");

        // 1. Tạo role ADMIN nếu chưa có
        Roles adminRole;
        if (!rolesRepository.existsByRoleName("ADMIN")) {
            adminRole = new Roles();
            adminRole.setRoleName("ADMIN");
            adminRole = rolesRepository.save(adminRole);
            System.out.println(">>> ROLE ADMIN CREATED <<<");
        } else {
            adminRole = rolesRepository.findAll()
                    .stream()
                    .filter(r -> r.getRoleName().equals("ADMIN"))
                    .findFirst()
                    .orElseThrow();
        }

        // 2. Tạo user admin nếu chưa có
        if (usersRepository.findByUsername("admin").isEmpty()) {
            Users admin = new Users();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setFullName("System Admin");
            admin.setRole(adminRole);
            admin.setIsActive(true);

            usersRepository.save(admin);
            System.out.println(">>> ADMIN USER CREATED <<<");
        }
    }
}
