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

        Roles adminRole = getOrCreateRole("ADMIN");
        Roles technicianRole = getOrCreateRole("TECHNICIAN");
        Roles accountantRole = getOrCreateRole("ACCOUNTANT");
        Roles enduserRole = getOrCreateRole("ENDUSER");

        if (usersRepository.findByUsername("admin").isEmpty()) {
            Users admin = new Users();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setFullName("System Admin");
            admin.setRole(adminRole);
            admin.setIsActive(true);
            usersRepository.save(admin);
            System.out.println(">>> ADMIN USER CREATED <<<");
        }

        if (usersRepository.findByUsername("tech1").isEmpty()) {
            Users tech = new Users();
            tech.setUsername("tech1");
            tech.setPassword(passwordEncoder.encode("password123"));
            tech.setFullName("Trần Văn Kỹ Thuật");
            tech.setRole(technicianRole);
            tech.setIsActive(true);
            usersRepository.save(tech);
            System.out.println(">>> TECHNICIAN USER CREATED <<<");
        }

        if (usersRepository.findByUsername("accountant").isEmpty()) {
            Users accountant = new Users();
            accountant.setUsername("accountant");
            accountant.setPassword(passwordEncoder.encode("password123"));
            accountant.setFullName("Lê Thị Kế Toán");
            accountant.setRole(accountantRole);
            accountant.setIsActive(true);
            usersRepository.save(accountant);
            System.out.println(">>> ACCOUNTANT USER CREATED <<<");
        }

        if (usersRepository.findByUsername("user1").isEmpty()) {
            Users endUser = new Users();
            endUser.setUsername("user1");
            endUser.setPassword(passwordEncoder.encode("password123"));
            endUser.setFullName("Nguyễn Thị Y Tá");
            endUser.setRole(enduserRole);
            endUser.setIsActive(true);
            usersRepository.save(endUser);
            System.out.println(">>> ENDUSER USER CREATED <<<");
        }
    }

    private Roles getOrCreateRole(String roleName) {
        return rolesRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Roles newRole = new Roles();
                    newRole.setRoleName(roleName);
                    Roles savedRole = rolesRepository.save(newRole);
                    System.out.println(">>> ROLE " + roleName + " CREATED <<<");
                    return savedRole;
                });
    }
}