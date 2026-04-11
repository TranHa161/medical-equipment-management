package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Roles;
import com.example.demo.model.Users;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Integer> {
    boolean existsByRoleName(String roleName);
    Roles findByRoleName(String roleName);
}

